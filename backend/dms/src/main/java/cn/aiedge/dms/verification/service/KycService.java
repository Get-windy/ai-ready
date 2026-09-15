package cn.aiedge.dms.verification.service;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.mapper.DmsChannelMapper;
import cn.aiedge.dms.common.enums.RiderTypeEnum;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.verification.dto.CertQueryDTO;
import cn.aiedge.dms.verification.dto.KycAuditDTO;
import cn.aiedge.dms.verification.dto.KycQueryDTO;
import cn.aiedge.dms.verification.dto.KycSubmitDTO;
import cn.aiedge.dms.verification.entity.DmsRiderCertificate;
import cn.aiedge.dms.verification.entity.DmsRiderVerification;
import cn.aiedge.dms.verification.enums.AlertTypeEnum;
import cn.aiedge.dms.verification.enums.CertStatusEnum;
import cn.aiedge.dms.verification.enums.CertTypeEnum;
import cn.aiedge.dms.verification.enums.VerifyStatusEnum;
import cn.aiedge.dms.verification.mapper.DmsRiderCertificateMapper;
import cn.aiedge.dms.verification.mapper.DmsRiderVerificationMapper;
import cn.aiedge.dms.verification.util.IdCardMasker;
import cn.aiedge.dms.verification.vo.EligibilityVO;
import cn.aiedge.dms.verification.vo.KycCertificateVO;
import cn.aiedge.dms.verification.vo.KycVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 骑手实名认证 / 资质（KYC）服务
 *
 * <p>能力：身份认证信息登记、证照管理（含有效期）、审核流（提交 → 审核 → 生效）、
 * 外部平台背书、到期预警、接单资质校验（「无资质不接单」唯一放行口径）。</p>
 *
 * <p>敏感信息最小化：身份证号落库前脱敏（前 3 后 4），系统不保存证件号原文。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycService {

    private final DmsRiderVerificationMapper verificationMapper;
    private final DmsRiderCertificateMapper certificateMapper;
    private final DmsRiderMapper riderMapper;
    private final DmsChannelMapper channelMapper;
    private final VerificationService verificationService;

    /** 到期提醒默认提前天数 */
    private static final int DEFAULT_WARN_DAYS = 30;

    // ==================== 查询 ====================

    /**
     * 实名认证台账分页
     *
     * <p>支持按配送员姓名/手机号、身份类型、认证状态、渠道、到期预警、是否具备资质过滤。</p>
     */
    public IPage<KycVO> page(KycQueryDTO q) {
        LambdaQueryWrapper<DmsRiderVerification> wrapper = new LambdaQueryWrapper<DmsRiderVerification>()
                .like(hasText(q.getRiderName()), DmsRiderVerification::getRiderName, q.getRiderName())
                .eq(q.getRiderType() != null, DmsRiderVerification::getRiderType, q.getRiderType())
                .eq(q.getVerifyStatus() != null, DmsRiderVerification::getVerifyStatus, q.getVerifyStatus())
                .eq(q.getChannelId() != null, DmsRiderVerification::getChannelId, q.getChannelId())
                .ge(q.getStartDate() != null, DmsRiderVerification::getCreateTime,
                        q.getStartDate() != null ? q.getStartDate().atStartOfDay() : null)
                .le(q.getEndDate() != null, DmsRiderVerification::getCreateTime,
                        q.getEndDate() != null ? LocalDateTime.of(q.getEndDate(), LocalTime.MAX) : null)
                .orderByDesc(DmsRiderVerification::getCreateTime);

        // 手机号需回查 dms_rider（台账只存姓名快照）
        if (hasText(q.getRiderPhone())) {
            List<Long> riderIds = riderMapper.selectList(
                            new LambdaQueryWrapper<DmsRider>().like(DmsRider::getPhone, q.getRiderPhone()))
                    .stream().map(DmsRider::getId).collect(Collectors.toList());
            if (riderIds.isEmpty()) {
                return new Page<>(q.getPage(), q.getSize());
            }
            wrapper.in(DmsRiderVerification::getRiderId, riderIds);
        }

        // 到期预警 / 具备资质：先解析命中集合，再按集合过滤（避免复杂子查询）
        if (Boolean.TRUE.equals(q.getExpiring())) {
            Set<Long> ids = resolveExpiringVerificationIds(null);
            if (ids.isEmpty()) {
                return new Page<>(q.getPage(), q.getSize());
            }
            wrapper.in(DmsRiderVerification::getId, ids);
        }
        if (Boolean.TRUE.equals(q.getEligible())) {
            wrapper.eq(DmsRiderVerification::getVerifyStatus, VerifyStatusEnum.APPROVED.getValue())
                    .and(w -> w.isNull(DmsRiderVerification::getEndorseExpireDate)
                            .or().ge(DmsRiderVerification::getEndorseExpireDate, LocalDate.now()));
        }

        IPage<DmsRiderVerification> raw = verificationMapper.selectPage(new Page<>(q.getPage(), q.getSize()), wrapper);
        return raw.convert(this::decorate);
    }

    /** 台账详情（含证照明细） */
    public KycVO detail(Long id) {
        DmsRiderVerification entity = verificationMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("实名认证记录不存在: " + id);
        }
        return decorate(entity);
    }

    /** 按配送员取台账（不存在返回 null，供表单回填） */
    public KycVO getByRiderId(Long riderId) {
        DmsRiderVerification entity = verificationMapper.selectOne(
                new LambdaQueryWrapper<DmsRiderVerification>()
                        .eq(DmsRiderVerification::getRiderId, riderId)
                        .last("LIMIT 1"));
        return entity == null ? null : decorate(entity);
    }

    /**
     * 接单资质校验（唯一放行口径）
     *
     * <p>《调度任务》指派/改派、《订单池》抢单在落库前调用；
     * 未通过认证、认证过期、证照过期、背书过期均不可接单。</p>
     */
    public EligibilityVO eligibility(Long riderId) {
        DmsRiderVerification entity = verificationMapper.selectOne(
                new LambdaQueryWrapper<DmsRiderVerification>()
                        .eq(DmsRiderVerification::getRiderId, riderId)
                        .last("LIMIT 1"));
        if (entity == null) {
            EligibilityVO vo = new EligibilityVO();
            vo.setRiderId(riderId);
            vo.setEligible(false);
            vo.setVerifyStatusText("未提交");
            vo.setReasons(List.of("该配送员尚未提交实名认证"));
            return vo;
        }
        return buildEligibility(entity, listCertificates(entity.getId()));
    }

    /**
     * 「无资质不接单」门控：指派 / 改派 / 抢单前调用
     *
     * <p>由租户参数 {@code verification.eligibility.enforce} 控制是否强制（默认强制）。</p>
     *
     * @throws BusinessException 不具备资质时抛出，消息含具体原因
     */
    public void assertEligible(Long riderId) {
        if (!isEnforceEnabled()) {
            return;
        }
        EligibilityVO vo = eligibility(riderId);
        if (!Boolean.TRUE.equals(vo.getEligible())) {
            throw BusinessException.badRequest("配送员不具备接单资质：" + String.join("；", vo.getReasons()));
        }
    }

    /**
     * 是否强制资质校验（租户参数 {@code verification.eligibility.enforce}）
     *
     * <p>缺省关闭：存量配送员尚未补齐 KYC 台账，强行开启会让全部指派/抢单失败。
     * 运营在「配送参数」中开启后再走「无资质不接单」硬门控。</p>
     */
    public boolean isEnforceEnabled() {
        return verificationService.paramBool(null, VerificationService.PARAM_ELIGIBILITY_ENFORCE, false);
    }

    /** 用已加载的台账 + 证照计算资质（避免列表逐行重复查询） */
    private EligibilityVO buildEligibility(DmsRiderVerification entity, List<DmsRiderCertificate> certs) {
        EligibilityVO vo = new EligibilityVO();
        vo.setRiderId(entity.getRiderId());
        vo.setVerifyStatus(entity.getVerifyStatus());
        vo.setVerifyStatusText(VerifyStatusEnum.fromValue(entity.getVerifyStatus()).getDescription());
        List<String> reasons = new ArrayList<>();

        if (!VerifyStatusEnum.eligible(entity.getVerifyStatus())) {
            reasons.add("实名认证状态为「" + vo.getVerifyStatusText() + "」，不可接单");
        }
        if (entity.getEndorseExpireDate() != null && entity.getEndorseExpireDate().isBefore(LocalDate.now())) {
            reasons.add("平台背书/资质已于 " + entity.getEndorseExpireDate() + " 到期");
        }
        List<String> expiredCerts = certs.stream()
                .filter(c -> c.getExpireDate() != null && c.getExpireDate().isBefore(LocalDate.now()))
                .map(c -> CertTypeEnum.text(c.getCertType()))
                .collect(Collectors.toList());
        if (!expiredCerts.isEmpty()) {
            reasons.add("证照已过期：" + String.join("、", expiredCerts));
        }

        vo.setReasons(reasons);
        vo.setEligible(reasons.isEmpty());
        return vo;
    }

    /**
     * 证照核验：按**证照维度**平铺分页（运营日常看的是到期清单，而非一条条台账）
     *
     * <p>到期判定复用租户参数 {@code kyc.cert.expire.warn.days}（默认 30 天）。</p>
     */
    public IPage<KycCertificateVO> pageCertificates(CertQueryDTO q) {
        LambdaQueryWrapper<DmsRiderCertificate> wrapper = new LambdaQueryWrapper<DmsRiderCertificate>()
                .eq(q.getRiderId() != null, DmsRiderCertificate::getRiderId, q.getRiderId())
                .eq(q.getCertType() != null, DmsRiderCertificate::getCertType, q.getCertType())
                .eq(q.getVerifyStatus() != null, DmsRiderCertificate::getVerifyStatus, q.getVerifyStatus())
                .orderByAsc(DmsRiderCertificate::getExpireDate);

        // 按持证人姓名过滤：先回查台账（证照表只存 riderId）
        if (hasText(q.getRiderName())) {
            List<Long> riderIds = riderMapper.selectList(
                            new LambdaQueryWrapper<DmsRider>().like(DmsRider::getRealName, q.getRiderName()))
                    .stream().map(DmsRider::getId).collect(Collectors.toList());
            if (riderIds.isEmpty()) {
                return new Page<>(q.getPage(), q.getSize());
            }
            wrapper.in(DmsRiderCertificate::getRiderId, riderIds);
        }

        LocalDate today = LocalDate.now();
        if (Boolean.TRUE.equals(q.getExpiring())) {
            // 提醒期内 或 已过期（无有效期的不算）
            LocalDate warnDate = today.plusDays(warnDays(null));
            wrapper.isNotNull(DmsRiderCertificate::getExpireDate)
                    .le(DmsRiderCertificate::getExpireDate, warnDate);
        }

        IPage<DmsRiderCertificate> raw = certificateMapper.selectPage(
                new Page<>(q.getPage(), q.getSize()), wrapper);
        return raw.convert(cert -> toCertVo(cert, today));
    }

    private KycCertificateVO toCertVo(DmsRiderCertificate cert, LocalDate today) {
        KycCertificateVO vo = new KycCertificateVO();
        BeanUtils.copyProperties(cert, vo);
        vo.setCertTypeText(CertTypeEnum.text(cert.getCertType()));
        vo.setVerifyStatusText(CertStatusEnum.text(cert.getVerifyStatus()));
        DmsRider rider = cert.getRiderId() == null ? null : riderMapper.selectById(cert.getRiderId());
        vo.setRiderName(rider == null ? null : rider.getRealName());
        vo.setRiderPhone(rider == null ? null : rider.getPhone());
        if (cert.getExpireDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, cert.getExpireDate());
            vo.setDaysToExpire(days);
            vo.setExpiring(days <= warnDays(cert.getTenantId()));
        } else {
            vo.setExpiring(false);
        }
        return vo;
    }

    // ==================== 提交 / 审核 ====================

    /**
     * 提交实名认证 / 资质材料
     *
     * <p>待提交/已驳回可重复提交；已通过需先驳回或失效再改（保证审核留痕唯一）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(KycSubmitDTO dto) {
        DmsRider rider = riderMapper.selectById(dto.getRiderId());
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在: " + dto.getRiderId());
        }

        DmsRiderVerification entity = dto.getId() != null ? verificationMapper.selectById(dto.getId()) : null;
        if (entity == null) {
            entity = verificationMapper.selectOne(new LambdaQueryWrapper<DmsRiderVerification>()
                    .eq(DmsRiderVerification::getRiderId, dto.getRiderId())
                    .last("LIMIT 1"));
        }
        if (entity != null && VerifyStatusEnum.APPROVED.getValue() == entity.getVerifyStatus()) {
            throw BusinessException.badRequest("该配送员实名认证已通过，如需变更请先驳回");
        }

        boolean isNew = entity == null;
        if (isNew) {
            entity = new DmsRiderVerification();
        }

        entity.setRiderId(rider.getId());
        entity.setRiderName(rider.getRealName());
        entity.setRiderType(rider.getRiderType());
        entity.setChannelId(rider.getChannelId());
        if (rider.getChannelId() != null) {
            DmsChannel channel = channelMapper.selectById(rider.getChannelId());
            entity.setChannelName(channel != null ? channel.getChannelName() : null);
        } else {
            entity.setChannelName(null);
        }
        entity.setRealName(hasText(dto.getRealName()) ? dto.getRealName() : rider.getRealName());
        // 存疑：只落脱敏值；已是脱敏值（含 *）时不重复处理，避免破坏数据
        if (hasText(dto.getIdCardNo()) && !IdCardMasker.isMasked(dto.getIdCardNo())) {
            entity.setIdCardNo(IdCardMasker.mask(dto.getIdCardNo()));
        }
        entity.setIdCardUrls(joinIdCardUrls(dto.getIdCardFrontUrl(), dto.getIdCardBackUrl()));
        entity.setEndorseOrg(dto.getEndorseOrg());
        entity.setEndorseResult(dto.getEndorseResult());
        entity.setEndorseExpireDate(dto.getEndorseExpireDate());
        entity.setRemark(dto.getRemark());
        entity.setVerifyStatus(Boolean.FALSE.equals(dto.getSubmitAudit())
                ? VerifyStatusEnum.DRAFT.getValue()
                : VerifyStatusEnum.PENDING.getValue());

        if (isNew) {
            verificationMapper.insert(entity);
        } else {
            verificationMapper.updateById(entity);
        }

        replaceCertificates(entity, dto.getCertificates());
        syncRiderStatus(entity);

        log.info("实名认证已提交, id={}, riderId={}, status={}", entity.getId(), rider.getId(), entity.getVerifyStatus());
        return entity.getId();
    }

    /**
     * 审核实名认证
     *
     * <p>通过 → 状态置「已通过」并写入生效时间；驳回 → 置「已驳回」且审核意见必填。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, KycAuditDTO dto) {
        DmsRiderVerification entity = verificationMapper.selectById(id);
        if (entity == null) {
            throw BusinessException.notFound("实名认证记录不存在: " + id);
        }
        if (VerifyStatusEnum.PENDING.getValue() != entity.getVerifyStatus()
                && VerifyStatusEnum.EXPIRED.getValue() != entity.getVerifyStatus()) {
            throw BusinessException.badRequest("当前状态不允许审核: "
                    + VerifyStatusEnum.fromValue(entity.getVerifyStatus()).getDescription());
        }

        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        if (!approved && !hasText(dto.getAuditRemark())) {
            throw BusinessException.badRequest("驳回时必须填写审核意见");
        }

        if (dto.getEndorseExpireDate() != null) {
            entity.setEndorseExpireDate(dto.getEndorseExpireDate());
        }
        if (dto.getCertificates() != null) {
            replaceCertificates(entity, dto.getCertificates());
        }
        if (approved) {
            entity.setVerifyStatus(VerifyStatusEnum.APPROVED.getValue());
            entity.setEffectiveTime(LocalDateTime.now());
        } else {
            entity.setVerifyStatus(VerifyStatusEnum.REJECTED.getValue());
        }
        entity.setAuditBy(SecurityUtils.getCurrentUserId());
        entity.setAuditTime(LocalDateTime.now());
        entity.setAuditRemark(dto.getAuditRemark());
        verificationMapper.updateById(entity);

        // 审核即校验证照时效，过期证照标注
        refreshCertStatus(entity.getId());
        syncRiderStatus(entity);

        log.info("实名认证已审核, id={}, approved={}, auditor={}", id, approved, VerificationService.currentOperator());
    }

    // ==================== 到期扫描（定时任务） ====================

    /**
     * 扫描证照 / 平台背书到期：标注证照状态，认证已通过但背书过期 → 置「已过期」并产生预警
     *
     * @param warnDays 提前提醒天数；null 时取租户配置
     * @return 新增预警数
     */
    @Transactional(rollbackFor = Exception.class)
    public int scanExpiry(Integer warnDays) {
        LocalDate today = LocalDate.now();
        List<DmsRiderVerification> all = verificationMapper.selectList(
                new LambdaQueryWrapper<DmsRiderVerification>().isNotNull(DmsRiderVerification::getRiderId));
        int alerts = 0;

        for (DmsRiderVerification entity : all) {
            int warn = warnDays != null ? warnDays : warnDays(entity.getTenantId());
            LocalDate warnDate = today.plusDays(warn);

            // 1. 证照状态刷新 + 到期预警
            List<DmsRiderCertificate> certs = listCertificates(entity.getId());
            for (DmsRiderCertificate cert : certs) {
                if (cert.getExpireDate() == null) {
                    continue;
                }
                int target = cert.getExpireDate().isBefore(today)
                        ? CertStatusEnum.EXPIRED.getValue()
                        : CertStatusEnum.VALID.getValue();
                if (!Objects.equals(cert.getVerifyStatus(), target)) {
                    cert.setVerifyStatus(target);
                    certificateMapper.updateById(cert);
                }
                if (!cert.getExpireDate().isAfter(warnDate)) {
                    boolean created = verificationService.raiseKycAlert(entity.getTenantId(), entity.getRiderId(),
                            entity.getRiderName(), AlertTypeEnum.CERT_EXPIRING.getValue(), 2,
                            CertTypeEnum.text(cert.getCertType()) + "（"
                                    + (cert.getCertNo() == null ? "无编号" : cert.getCertNo()) + "）"
                                    + (cert.getExpireDate().isBefore(today) ? "已于 " : "将于 ")
                                    + cert.getExpireDate() + " 到期，请及时更新");
                    if (created) {
                        alerts++;
                    }
                }
            }

            // 2. 平台背书过期 → 认证置「已过期」
            if (entity.getEndorseExpireDate() != null && entity.getEndorseExpireDate().isBefore(today)) {
                if (VerifyStatusEnum.APPROVED.getValue() == entity.getVerifyStatus()) {
                    entity.setVerifyStatus(VerifyStatusEnum.EXPIRED.getValue());
                    verificationMapper.updateById(entity);
                    syncRiderStatus(entity);
                }
                boolean created = verificationService.raiseKycAlert(entity.getTenantId(), entity.getRiderId(),
                        entity.getRiderName(), AlertTypeEnum.CERT_EXPIRING.getValue(), 2,
                        "平台背书/资质已于 " + entity.getEndorseExpireDate() + " 到期，实名认证失效");
                if (created) {
                    alerts++;
                }
            } else if (entity.getEndorseExpireDate() != null
                    && !entity.getEndorseExpireDate().isAfter(warnDate)
                    && VerifyStatusEnum.APPROVED.getValue() == entity.getVerifyStatus()) {
                boolean created = verificationService.raiseKycAlert(entity.getTenantId(), entity.getRiderId(),
                        entity.getRiderName(), AlertTypeEnum.CERT_EXPIRING.getValue(), 2,
                        "平台背书/资质将于 " + entity.getEndorseExpireDate() + " 到期，请提前续签");
                if (created) {
                    alerts++;
                }
            }
        }

        if (alerts > 0) {
            log.warn("证照/资质到期扫描完成，新增预警 {} 条", alerts);
        }
        return alerts;
    }

    // ==================== 内部方法 ====================

    /** 台账行装饰：类型/状态文本、手机号、证照、到期数、资质判定 */
    private KycVO decorate(DmsRiderVerification entity) {
        KycVO vo = new KycVO();
        BeanUtils.copyProperties(entity, vo);

        vo.setRiderTypeText(entity.getRiderType() == null ? "-" : RiderTypeEnum.fromValue(entity.getRiderType()).getDescription());
        vo.setVerifyStatusText(VerifyStatusEnum.fromValue(entity.getVerifyStatus()).getDescription());

        DmsRider rider = entity.getRiderId() == null ? null : riderMapper.selectById(entity.getRiderId());
        vo.setRiderPhone(rider != null ? rider.getPhone() : null);

        List<DmsRiderCertificate> certs = listCertificates(entity.getId());
        vo.setCertificates(certs);
        vo.setCertCount(certs.size());

        LocalDate today = LocalDate.now();
        LocalDate warnDate = today.plusDays(warnDays(entity.getTenantId()));
        long expiring = certs.stream()
                .filter(c -> c.getExpireDate() != null && !c.getExpireDate().isAfter(warnDate))
                .count();
        if (entity.getEndorseExpireDate() != null && !entity.getEndorseExpireDate().isAfter(warnDate)) {
            expiring++;
        }
        vo.setExpiringCertCount((int) expiring);

        EligibilityVO elig = buildEligibility(entity, certs);
        vo.setEligible(elig.getEligible());
        vo.setIneligibleReason(elig.getReasons() == null || elig.getReasons().isEmpty()
                ? null : String.join("；", elig.getReasons()));
        return vo;
    }

    private List<DmsRiderCertificate> listCertificates(Long verificationId) {
        if (verificationId == null) {
            return Collections.emptyList();
        }
        return certificateMapper.selectList(new LambdaQueryWrapper<DmsRiderCertificate>()
                .eq(DmsRiderCertificate::getVerificationId, verificationId)
                .orderByAsc(DmsRiderCertificate::getCertType));
    }

    /** 证照整体替换（与线路配送区域子表同口径：编辑即整体替换） */
    private void replaceCertificates(DmsRiderVerification entity, List<KycSubmitDTO.CertificateDTO> certs) {
        if (certs == null) {
            return;
        }
        certificateMapper.delete(new LambdaQueryWrapper<DmsRiderCertificate>()
                .eq(DmsRiderCertificate::getVerificationId, entity.getId()));
        LocalDate today = LocalDate.now();
        for (KycSubmitDTO.CertificateDTO dto : certs) {
            if (dto == null) {
                continue;
            }
            DmsRiderCertificate cert = new DmsRiderCertificate();
            cert.setVerificationId(entity.getId());
            cert.setRiderId(entity.getRiderId());
            cert.setCertType(dto.getCertType() == null ? CertTypeEnum.OTHER.getValue() : dto.getCertType());
            cert.setCertNo(dto.getCertNo());
            cert.setIssueDate(dto.getIssueDate());
            cert.setExpireDate(dto.getExpireDate());
            cert.setCertUrl(dto.getCertUrl());
            cert.setRemark(dto.getRemark());
            cert.setVerifyStatus(dto.getExpireDate() != null && dto.getExpireDate().isBefore(today)
                    ? CertStatusEnum.EXPIRED.getValue()
                    : CertStatusEnum.VALID.getValue());
            certificateMapper.insert(cert);
        }
    }

    /** 审核后按证照时效刷新证照状态 */
    private void refreshCertStatus(Long verificationId) {
        LocalDate today = LocalDate.now();
        for (DmsRiderCertificate cert : listCertificates(verificationId)) {
            int target = cert.getExpireDate() != null && cert.getExpireDate().isBefore(today)
                    ? CertStatusEnum.EXPIRED.getValue()
                    : CertStatusEnum.VALID.getValue();
            if (!Objects.equals(cert.getVerifyStatus(), target)) {
                cert.setVerifyStatus(target);
                certificateMapper.updateById(cert);
            }
        }
    }

    /** 回写 dms_rider.verify_status（0-待审核 1-已通过 2-已拒绝/失效） */
    private void syncRiderStatus(DmsRiderVerification entity) {
        if (entity.getRiderId() == null) {
            return;
        }
        DmsRider rider = riderMapper.selectById(entity.getRiderId());
        if (rider == null) {
            return;
        }
        int riderStatus = switch (VerifyStatusEnum.fromValue(entity.getVerifyStatus())) {
            case APPROVED -> 1;
            case DRAFT, PENDING -> 0;
            case REJECTED, EXPIRED -> 2;
        };
        rider.setVerifyStatus(riderStatus);
        riderMapper.updateById(rider);
    }

    /** 解析「在提醒期内 or 已过期」的台账ID集合 */
    private Set<Long> resolveExpiringVerificationIds(Integer warnDays) {
        LocalDate warnDate = LocalDate.now().plusDays(warnDays == null ? DEFAULT_WARN_DAYS : warnDays);
        Set<Long> ids = certificateMapper.selectList(new LambdaQueryWrapper<DmsRiderCertificate>()
                        .isNotNull(DmsRiderCertificate::getExpireDate)
                        .le(DmsRiderCertificate::getExpireDate, warnDate))
                .stream().map(DmsRiderCertificate::getVerificationId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        verificationMapper.selectList(new LambdaQueryWrapper<DmsRiderVerification>()
                        .isNotNull(DmsRiderVerification::getEndorseExpireDate)
                        .le(DmsRiderVerification::getEndorseExpireDate, warnDate))
                .forEach(v -> ids.add(v.getId()));
        return ids;
    }

    private int warnDays(Long tenantId) {
        return verificationService.paramInt(tenantId, VerificationService.PARAM_CERT_WARN_DAYS, DEFAULT_WARN_DAYS);
    }

    /** 身份证正反面合并为 JSON（系统只存影像 URL，不落证件号原文） */
    private String joinIdCardUrls(String front, String back) {
        if (!hasText(front) && !hasText(back)) {
            return null;
        }
        Map<String, String> map = new HashMap<>();
        map.put("front", front == null ? "" : front);
        map.put("back", back == null ? "" : back);
        return "{\"front\":\"" + escape(map.get("front")) + "\",\"back\":\"" + escape(map.get("back")) + "\"}";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
