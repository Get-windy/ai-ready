package cn.aiedge.dms.rider.service;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.base.entity.SysDept;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysDeptMapper;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.dms.channel.entity.DmsChannel;
import cn.aiedge.dms.channel.mapper.DmsChannelMapper;
import cn.aiedge.dms.common.enums.RiderTypeEnum;
import cn.aiedge.dms.rider.dto.RiderQuery;
import cn.aiedge.dms.rider.dto.RiderVO;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.mapper.DmsRiderMapper;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.aiedge.dms.verification.mapper.DmsRiderVehicleBindingMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配送员管理服务（金标准）
 *
 * <p>业务口径见《配送员管理开发文档》：
 * ① 两类运力同池（企业员工 / 外部平台配送员）在列表与调度侧同权比较；
 * ② 资质门控：审核未通过或证照过期的配送员不可被置为可接单状态；
 * ③ 在线判定优先用轨迹上报心跳（lastReportTime），人工状态作为补充。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiderService {

    /** 心跳在线判定窗口（分钟）：最近一次位置上报在此窗口内视为在线 */
    private static final int ONLINE_HEARTBEAT_MINUTES = 5;
    /** 资质到期提醒默认天数 */
    private static final int DEFAULT_QUALIFY_ALERT_DAYS = 30;
    /** 编号前缀 */
    private static final String RIDER_NO_PREFIX = "PSY";

    private final DmsRiderMapper riderMapper;
    private final DmsChannelMapper channelMapper;
    private final DmsRiderVehicleBindingMapper bindingMapper;
    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;

    // ═══════════════════════════ 查询 ═══════════════════════════

    /**
     * 多条件分页（联查归属渠道 / 部门 / 系统账号 / 当前绑定车辆，并派生在线状态与资质到期）
     */
    public Page<RiderVO> pageQuery(RiderQuery query) {
        Page<DmsRider> page = new Page<>(
                query.getPageNum() == null ? 1 : query.getPageNum(),
                query.getPageSize() == null ? 20 : query.getPageSize());
        Page<DmsRider> result = riderMapper.selectPage(page, buildWrapper(query));
        return toVoPage(result);
    }

    /**
     * 不分页列表（导出 / 选择器 / 兼容旧调用）
     */
    public List<RiderVO> list(RiderQuery query) {
        List<DmsRider> rows = riderMapper.selectList(buildWrapper(query));
        return toVoList(rows, true);
    }

    /**
     * 详情
     */
    public RiderVO getDetail(Long id) {
        DmsRider rider = riderMapper.selectById(id);
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在");
        }
        // 详情（编辑回填）返回原始身份证号，列表按合规要求脱敏
        return toVoList(Collections.singletonList(rider), false).get(0);
    }

    private LambdaQueryWrapper<DmsRider> buildWrapper(RiderQuery query) {
        LambdaQueryWrapper<DmsRider> wrapper = new LambdaQueryWrapper<>();
        if (query == null) {
            return wrapper.orderByDesc(DmsRider::getCreateTime);
        }

        String keyword = trim(query.getKeyword());
        if (keyword != null) {
            wrapper.and(w -> w.like(DmsRider::getRiderNo, keyword)
                    .or().like(DmsRider::getRealName, keyword)
                    .or().like(DmsRider::getPhone, keyword));
        }
        if (trim(query.getRealName()) != null) {
            wrapper.like(DmsRider::getRealName, trim(query.getRealName()));
        }
        if (trim(query.getPhone()) != null) {
            wrapper.like(DmsRider::getPhone, trim(query.getPhone()));
        }
        if (query.getRiderTypes() != null && !query.getRiderTypes().isEmpty()) {
            wrapper.in(DmsRider::getRiderType, query.getRiderTypes());
        }
        if (query.getChannelId() != null) {
            wrapper.eq(DmsRider::getChannelId, query.getChannelId());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(DmsRider::getDeptId, query.getDeptId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(DmsRider::getStatus, query.getStatus());
        }
        if (query.getVerifyStatus() != null) {
            wrapper.eq(DmsRider::getVerifyStatus, query.getVerifyStatus());
        }
        if (query.getOnlineStatus() != null) {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(ONLINE_HEARTBEAT_MINUTES);
            if (query.getOnlineStatus() == 1) {
                wrapper.ge(DmsRider::getLastReportTime, threshold);
            } else {
                wrapper.and(w -> w.lt(DmsRider::getLastReportTime, threshold)
                        .or().isNull(DmsRider::getLastReportTime));
            }
        }
        if (query.getQualifyExpireAlert() != null && query.getQualifyExpireAlert() == 1) {
            int days = query.getQualifyExpireDays() == null || query.getQualifyExpireDays() <= 0
                    ? DEFAULT_QUALIFY_ALERT_DAYS : query.getQualifyExpireDays();
            wrapper.isNotNull(DmsRider::getQualificationExpireDate)
                    .le(DmsRider::getQualificationExpireDate, LocalDate.now().plusDays(days));
        }
        if (query.getCreateTimeStart() != null) {
            wrapper.ge(DmsRider::getCreateTime, query.getCreateTimeStart().atStartOfDay());
        }
        if (query.getCreateTimeEnd() != null) {
            wrapper.le(DmsRider::getCreateTime, query.getCreateTimeEnd().atTime(23, 59, 59));
        }
        applySort(wrapper, query);
        return wrapper;
    }

    /** 服务端排序（白名单，避免任意字段注入） */
    private void applySort(LambdaQueryWrapper<DmsRider> wrapper, RiderQuery query) {
        boolean asc = !"desc".equalsIgnoreCase(query.getSortOrder());
        String field = query.getSortField();
        if (field == null || field.isEmpty()) {
            wrapper.orderByDesc(DmsRider::getCreateTime);
            return;
        }
        switch (field) {
            case "riderNo" -> wrapper.orderBy(true, asc, DmsRider::getRiderNo);
            case "realName" -> wrapper.orderBy(true, asc, DmsRider::getRealName);
            case "createTime" -> wrapper.orderBy(true, asc, DmsRider::getCreateTime);
            case "ratingScore" -> wrapper.orderBy(true, asc, DmsRider::getRatingScore);
            case "totalOrders" -> wrapper.orderBy(true, asc, DmsRider::getTotalOrders);
            case "punctualRate" -> wrapper.orderBy(true, asc, DmsRider::getPunctualRate);
            default -> wrapper.orderByDesc(DmsRider::getCreateTime);
        }
    }

    // ═══════════════════════════ 新增 / 修改 ═══════════════════════════

    @Transactional(rollbackFor = Exception.class)
    public RiderVO create(DmsRider dto) {
        DmsRider rider = new DmsRider();
        copyEditableFields(dto, rider);
        validateByType(rider, true);
        rider.setRiderNo(trim(dto.getRiderNo()) == null ? nextCode() : trim(dto.getRiderNo()));
        assertRiderNoUnique(rider.getRiderNo(), null);
        rider.setTenantId(currentTenantId());
        rider.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        rider.setVerifyStatus(0);          // 新增一律待审核（资质门控前置）
        rider.setRatingScore(new BigDecimal("5.00"));
        rider.setTotalOrders(0);
        rider.setTodayOrders(0);
        rider.setPunctualRate(new BigDecimal("100.00"));
        riderMapper.insert(rider);
        log.info("新增配送员: id={}, no={}, name={}, type={}", rider.getId(), rider.getRiderNo(),
                rider.getRealName(), rider.getRiderType());
        return toVoList(Collections.singletonList(rider), false).get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public RiderVO update(Long id, DmsRider dto) {
        DmsRider rider = riderMapper.selectById(id);
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在");
        }
        copyEditableFields(dto, rider);
        validateByType(rider, false);
        String riderNo = trim(dto.getRiderNo());
        if (riderNo != null) {
            assertRiderNoUnique(riderNo, id);
            rider.setRiderNo(riderNo);
        }
        riderMapper.updateById(rider);
        log.info("修改配送员: id={}, name={}", id, rider.getRealName());
        return toVoList(Collections.singletonList(rider), false).get(0);
    }

    /** 可编辑字段（编号/审核状态/绩效统计不在此列，避免前端覆盖系统口径） */
    private void copyEditableFields(DmsRider dto, DmsRider rider) {
        rider.setRealName(trim(dto.getRealName()));
        rider.setPhone(trim(dto.getPhone()));
        rider.setIdCard(trim(dto.getIdCard()));
        rider.setAvatarUrl(dto.getAvatarUrl());
        rider.setRiderType(dto.getRiderType());
        rider.setUserId(dto.getUserId());
        rider.setDeptId(dto.getDeptId());
        rider.setDeptName(trim(dto.getDeptName()));
        rider.setEntryDate(dto.getEntryDate());
        rider.setChannelId(dto.getChannelId());
        rider.setPlatformRiderId(trim(dto.getPlatformRiderId()));
        rider.setQualificationExpireDate(dto.getQualificationExpireDate());
        rider.setDriverLicense(trim(dto.getDriverLicense()));
        rider.setHealthCertNo(trim(dto.getHealthCertNo()));
        rider.setSettleMethod(dto.getSettleMethod());
        rider.setVehicleType(trim(dto.getVehicleType()));
        rider.setVehicleNo(trim(dto.getVehicleNo()));
        rider.setServiceRadius(dto.getServiceRadius());
        rider.setMaxConcurrent(dto.getMaxConcurrent());
        rider.setWorkHoursStart(dto.getWorkHoursStart());
        rider.setWorkHoursEnd(dto.getWorkHoursEnd());
        rider.setDepositAmount(dto.getDepositAmount());
        rider.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            rider.setStatus(dto.getStatus());
        }
    }

    /**
     * 按类型差异化必填校验（见《配送员管理开发文档》§3.2）
     */
    private void validateByType(DmsRider rider, boolean isCreate) {
        if (trim(rider.getRealName()) == null) {
            throw new BusinessException("请输入配送员姓名");
        }
        if (trim(rider.getPhone()) == null) {
            throw new BusinessException("请输入手机号");
        }
        if (!trim(rider.getPhone()).matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        if (rider.getRiderType() == null) {
            throw new BusinessException("请选择配送员类型");
        }
        RiderTypeEnum type = RiderTypeEnum.fromValue(rider.getRiderType());
        switch (type) {
            case OWN_STAFF -> {
                if (rider.getUserId() == null) {
                    throw new BusinessException("企业员工必须关联系统账号");
                }
            }
            case PLATFORM_RIDER -> {
                if (rider.getChannelId() == null) {
                    throw new BusinessException("外部平台配送员必须选择归属渠道");
                }
                if (trim(rider.getPlatformRiderId()) == null) {
                    throw new BusinessException("外部平台配送员必须填写平台骑手ID");
                }
            }
            case SOCIAL_DRIVER -> {
                if (trim(rider.getVehicleNo()) == null) {
                    throw new BusinessException("社会车辆司机必须填写车牌号");
                }
            }
            default -> { /* 众包兼职：无额外必填 */ }
        }
        if (isCreate && !Arrays.asList(0, 1, 2, 3).contains(rider.getStatus() == null ? 0 : rider.getStatus())) {
            throw new BusinessException("配送员状态不合法");
        }
    }

    private void assertRiderNoUnique(String riderNo, Long excludeId) {
        LambdaQueryWrapper<DmsRider> wrapper = new LambdaQueryWrapper<DmsRider>()
                .eq(DmsRider::getRiderNo, riderNo);
        if (excludeId != null) {
            wrapper.ne(DmsRider::getId, excludeId);
        }
        if (riderMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("配送员编号「" + riderNo + "」已存在");
        }
    }

    // ═══════════════════════════ 状态 / 审核 ═══════════════════════════

    /**
     * 状态流转（状态机 + 资质门控）：
     * 离线 ⇄ 空闲 → 忙碌 → 休息；置为「空闲/忙碌」（可接单）要求审核通过且资质未过期。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        DmsRider rider = riderMapper.selectById(id);
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在");
        }
        assertStatusTransition(rider, status);
        rider.setStatus(status);
        riderMapper.updateById(rider);
        log.info("配送员状态更新: id={}, status={}", id, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择配送员");
        }
        int ok = 0;
        for (Long id : ids) {
            updateStatus(id, status);
            ok++;
        }
        log.info("批量更新配送员状态: count={}, status={}", ok, status);
        return ok;
    }

    private void assertStatusTransition(DmsRider rider, Integer status) {
        if (status == null || !Arrays.asList(0, 1, 2, 3).contains(status)) {
            throw new BusinessException("配送员状态不合法");
        }
        if (Objects.equals(rider.getStatus(), status)) {
            return;
        }
        // 可接单状态（空闲/忙碌）→ 资质门控
        if (status == 1 || status == 2) {
            if (!Objects.equals(rider.getVerifyStatus(), 1)) {
                throw new BusinessException("配送员审核未通过，不可置为可接单状态");
            }
            if (rider.getQualificationExpireDate() != null
                    && rider.getQualificationExpireDate().isBefore(LocalDate.now())) {
                throw new BusinessException("配送员资质已过期，不可接单，请先更新证照有效期");
            }
        }
    }

    /**
     * 审核（通过/拒绝 + 备注）
     */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Integer verifyStatus, String remark) {
        if (verifyStatus == null || (verifyStatus != 1 && verifyStatus != 2)) {
            throw new BusinessException("审核结果不合法（1-通过 2-拒绝）");
        }
        DmsRider rider = riderMapper.selectById(id);
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在");
        }
        rider.setVerifyStatus(verifyStatus);
        rider.setVerifyRemark(remark);
        if (verifyStatus == 2) {
            rider.setStatus(0); // 审核拒绝 → 强制离线（资质门控）
        }
        riderMapper.updateById(rider);
        log.info("配送员审核: id={}, verifyStatus={}", id, verifyStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchApprove(List<Long> ids, Integer verifyStatus, String remark) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择配送员");
        }
        for (Long id : ids) {
            approve(id, verifyStatus, remark);
        }
        log.info("批量审核配送员: count={}, verifyStatus={}", ids.size(), verifyStatus);
        return ids.size();
    }

    // ═══════════════════════════ 删除 ═══════════════════════════

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        DmsRider rider = riderMapper.selectById(id);
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在");
        }
        riderMapper.deleteById(id);
        log.info("删除配送员: id={}, no={}", id, rider.getRiderNo());
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择配送员");
        }
        int count = riderMapper.deleteBatchIds(ids);
        log.info("批量删除配送员: count={}", count);
        return count;
    }

    // ═══════════════════════════ 位置上报 ═══════════════════════════

    @Transactional(rollbackFor = Exception.class)
    public void updateLocation(Long riderId, BigDecimal lat, BigDecimal lng) {
        DmsRider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            throw BusinessException.notFound("配送员不存在");
        }
        rider.setCurrentLat(lat);
        rider.setCurrentLng(lng);
        rider.setLastReportTime(LocalDateTime.now());
        riderMapper.updateById(rider);
    }

    // ═══════════════════════════ 选择器 / 编号 ═══════════════════════════

    /**
     * 选择器数据源（调度指派 / 车辆绑定 / 线路共用）
     *
     * @param assignable 仅返回「可指派」的配送员（审核通过 + 资质未过期）
     */
    public List<Map<String, Object>> options(String keyword, boolean assignable) {
        RiderQuery query = new RiderQuery();
        query.setPageSize(200);
        query.setKeyword(keyword);
        query.setSortField("riderNo");
        query.setSortOrder("asc");
        List<DmsRider> rows = riderMapper.selectList(buildWrapper(query));
        List<Map<String, Object>> result = new ArrayList<>();
        for (DmsRider rider : rows) {
            if (assignable && !isAssignable(rider)) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", rider.getId());
            item.put("riderNo", rider.getRiderNo());
            item.put("realName", rider.getRealName());
            item.put("phone", rider.getPhone());
            item.put("riderType", rider.getRiderType());
            item.put("riderTypeText", RiderTypeEnum.fromValue(rider.getRiderType() == null ? 1 : rider.getRiderType()).getDescription());
            item.put("status", rider.getStatus());
            result.add(item);
        }
        return result;
    }

    /** 资质门控：审核通过 + 资质未过期（无有效期视为长期有效） */
    public boolean isAssignable(DmsRider rider) {
        if (!Objects.equals(rider.getVerifyStatus(), 1)) {
            return false;
        }
        return rider.getQualificationExpireDate() == null
                || !rider.getQualificationExpireDate().isBefore(LocalDate.now());
    }

    /**
     * 生成下一个配送员编号（PSY + 4 位序号，同租户内递增）
     */
    public String nextCode() {
        List<DmsRider> rows = riderMapper.selectList(new LambdaQueryWrapper<DmsRider>()
                .select(DmsRider::getRiderNo)
                .likeRight(DmsRider::getRiderNo, RIDER_NO_PREFIX));
        int max = 0;
        for (DmsRider row : rows) {
            String no = row.getRiderNo();
            if (no == null || no.length() <= RIDER_NO_PREFIX.length()) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(no.substring(RIDER_NO_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                // 非标准编号（人工改写）跳过
            }
        }
        return RIDER_NO_PREFIX + String.format("%04d", max + 1);
    }

    // ═══════════════════════════ 转换为 VO ═══════════════════════════

    private Page<RiderVO> toVoPage(Page<DmsRider> page) {
        Page<RiderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(toVoList(page.getRecords(), true));
        return voPage;
    }

    /**
     * 批量转换为 VO：一次性回填 渠道名称 / 部门 / 系统账号 / 当前绑定车辆，避免 N+1 查询。
     *
     * @param maskIdCard 是否脱敏身份证（列表 true / 详情与保存回显 false）
     */
    public List<RiderVO> toVoList(List<DmsRider> rows, boolean maskIdCard) {
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> channelIds = rows.stream().map(DmsRider::getChannelId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = rows.stream().map(DmsRider::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> deptIds = rows.stream().map(DmsRider::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<Long> riderIds = rows.stream().map(DmsRider::getId).filter(Objects::nonNull).collect(Collectors.toList());

        Map<Long, String> channelNames = channelIds.isEmpty() ? Collections.emptyMap()
                : channelMapper.selectBatchIds(channelIds).stream()
                    .collect(Collectors.toMap(DmsChannel::getId, c -> nullSafe(c.getChannelName()), (a, b) -> a));
        Map<Long, String> userNames = userIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(SysUser::getId,
                            u -> trim(u.getRealName()) == null ? nullSafe(u.getUsername()) : u.getRealName(),
                            (a, b) -> a));
        Map<Long, String> deptNames = deptIds.isEmpty() ? Collections.emptyMap()
                : sysDeptMapper.selectBatchIds(deptIds).stream()
                    .collect(Collectors.toMap(SysDept::getId, d -> nullSafe(d.getDeptName()), (a, b) -> a));
        Map<Long, String> vehiclePlates = riderIds.isEmpty() ? Collections.emptyMap()
                : bindingMapper.selectList(new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .in(DmsRiderVehicleBinding::getRiderId, riderIds)
                        .eq(DmsRiderVehicleBinding::getStatus, 1)
                        .orderByDesc(DmsRiderVehicleBinding::getBindTime)).stream()
                    .collect(Collectors.toMap(DmsRiderVehicleBinding::getRiderId,
                            b -> nullSafe(b.getPlateNo()), (a, b) -> a));

        return rows.stream().map(r -> toVo(r, channelNames, userNames, deptNames, vehiclePlates, maskIdCard))
                .collect(Collectors.toList());
    }

    private RiderVO toVo(DmsRider rider,
                         Map<Long, String> channelNames,
                         Map<Long, String> userNames,
                         Map<Long, String> deptNames,
                         Map<Long, String> vehiclePlates,
                         boolean maskIdCard) {
        RiderVO vo = new RiderVO();
        vo.setId(rider.getId());
        vo.setRiderNo(rider.getRiderNo());
        vo.setRealName(rider.getRealName());
        vo.setPhone(rider.getPhone());
        vo.setIdCard(maskIdCard ? maskIdCard(rider.getIdCard()) : rider.getIdCard());
        RiderTypeEnum type = RiderTypeEnum.fromValue(rider.getRiderType() == null ? 1 : rider.getRiderType());
        vo.setRiderType(rider.getRiderType());
        vo.setRiderTypeText(type.getDescription());
        vo.setStatus(rider.getStatus());
        vo.setStatusText(STATUS_TEXT.getOrDefault(rider.getStatus() == null ? 0 : rider.getStatus(), "未知"));
        vo.setVerifyStatus(rider.getVerifyStatus());
        vo.setVerifyStatusText(VERIFY_TEXT.getOrDefault(rider.getVerifyStatus() == null ? 0 : rider.getVerifyStatus(), "未知"));
        vo.setVerifyRemark(rider.getVerifyRemark());
        vo.setUserId(rider.getUserId());
        vo.setUserName(rider.getUserId() == null ? null : userNames.get(rider.getUserId()));
        vo.setDeptId(rider.getDeptId());
        vo.setDeptName(rider.getDeptName() != null ? rider.getDeptName()
                : (rider.getDeptId() == null ? null : deptNames.get(rider.getDeptId())));
        vo.setEntryDate(rider.getEntryDate());
        vo.setChannelId(rider.getChannelId());
        vo.setChannelName(rider.getChannelId() == null ? null : channelNames.get(rider.getChannelId()));
        vo.setPlatformRiderId(rider.getPlatformRiderId());
        vo.setQualificationExpireDate(rider.getQualificationExpireDate());
        if (rider.getQualificationExpireDate() != null) {
            long remain = ChronoUnit.DAYS.between(LocalDate.now(), rider.getQualificationExpireDate());
            vo.setQualificationRemainDays(remain);
            vo.setQualificationExpired(remain < 0);
        } else {
            vo.setQualificationExpired(false);
        }
        vo.setDriverLicense(rider.getDriverLicense());
        vo.setHealthCertNo(rider.getHealthCertNo());
        vo.setSettleMethod(rider.getSettleMethod());
        vo.setSettleMethodText(rider.getSettleMethod() == null ? null : SETTLE_TEXT.get(rider.getSettleMethod()));
        vo.setRatingScore(rider.getRatingScore());
        vo.setTotalOrders(rider.getTotalOrders());
        vo.setTodayOrders(rider.getTodayOrders());
        vo.setPunctualRate(rider.getPunctualRate());
        vo.setVehicleType(rider.getVehicleType());
        vo.setVehicleNo(rider.getVehicleNo());
        vo.setVehiclePlate(vehiclePlates.getOrDefault(rider.getId(),
                rider.getVehicleNo() == null ? "" : rider.getVehicleNo()));
        boolean online = rider.getLastReportTime() != null
                && rider.getLastReportTime().isAfter(LocalDateTime.now().minusMinutes(ONLINE_HEARTBEAT_MINUTES));
        vo.setOnlineStatus(online ? 1 : 0);
        vo.setOnlineStatusText(online ? "在线" : "离线");
        vo.setLastReportTime(rider.getLastReportTime());
        vo.setCurrentLat(rider.getCurrentLat());
        vo.setCurrentLng(rider.getCurrentLng());
        vo.setServiceRadius(rider.getServiceRadius());
        vo.setMaxConcurrent(rider.getMaxConcurrent());
        vo.setWorkHoursStart(rider.getWorkHoursStart());
        vo.setWorkHoursEnd(rider.getWorkHoursEnd());
        vo.setDepositAmount(rider.getDepositAmount());
        vo.setRemark(rider.getRemark());
        vo.setCreateTime(rider.getCreateTime());
        vo.setUpdateTime(rider.getUpdateTime());
        return vo;
    }

    /** 身份证脱敏：保留前 6 后 4 */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    // ═══════════════════════════ 工具 ═══════════════════════════

    private static final Map<Integer, String> STATUS_TEXT = Map.of(
            0, "离线", 1, "空闲", 2, "忙碌", 3, "休息");
    private static final Map<Integer, String> VERIFY_TEXT = Map.of(
            0, "待审核", 1, "已通过", 2, "已拒绝");
    private static final Map<Integer, String> SETTLE_TEXT = Map.of(
            1, "按单结算", 2, "月结", 3, "时段结算");

    private Long currentTenantId() {
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        return tenantId == null ? 0L : tenantId;
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /** 保留：调度侧按服务半径筛选可用配送员（沿用旧能力） */
    public List<DmsRider> getAvailableRiders(BigDecimal lat, BigDecimal lng, BigDecimal radius) {
        LambdaQueryWrapper<DmsRider> wrapper = new LambdaQueryWrapper<DmsRider>()
                .eq(DmsRider::getStatus, 1)
                .eq(DmsRider::getVerifyStatus, 1)
                .ge(DmsRider::getServiceRadius, radius)
                .orderByDesc(DmsRider::getRatingScore);
        return riderMapper.selectList(wrapper);
    }

    /** 供其它模块复用：按 ID 批量取配送员名称映射 */
    public Map<Long, String> nameMap(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return riderMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(DmsRider::getId, r -> nullSafe(r.getRealName()), (a, b) -> a,
                        LinkedHashMap::new));
    }

    /** 供其它模块复用：配送员类型文案 */
    public static String typeText(Integer riderType) {
        return RiderTypeEnum.fromValue(riderType == null ? 1 : riderType).getDescription();
    }

    // ═══════════════════════════ Excel 导入（真实落库） ═══════════════════════════

    /**
     * Excel 导入（逐行校验 + 真实落库），返回 {total, success, failure, errors}
     *
     * <p>不标注 @Transactional：逐行独立落库，避免单行失败导致整批回滚。</p>
     */
    public Map<String, Object> importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的 Excel 文件");
        }
        List<String> errors = new ArrayList<>();
        int total = 0;
        int success = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headRow = sheet.getRow(sheet.getFirstRowNum());
            if (headRow == null) {
                return importResult(0, 0, List.of("模板缺少表头行"));
            }
            Map<Integer, String> headerMap = new HashMap<>();
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : headRow) {
                String field = importField(formatter.formatCellValue(cell));
                if (field != null) {
                    headerMap.put(cell.getColumnIndex(), field);
                }
            }
            if (!headerMap.containsValue("realName") || !headerMap.containsValue("phone")) {
                return importResult(0, 0, List.of("模板缺少「姓名」或「手机号」列"));
            }

            for (int rowIdx = sheet.getFirstRowNum() + 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new HashMap<>();
                headerMap.forEach((col, field) -> values.put(field, formatter.formatCellValue(row.getCell(col)).trim()));
                if (!hasText(values.get("realName")) && !hasText(values.get("phone"))) {
                    continue; // 整行为空
                }
                total++;
                int rowNo = rowIdx + 1;
                try {
                    DmsRider rider = new DmsRider();
                    rider.setRealName(blankToNull(values.get("realName")));
                    rider.setPhone(blankToNull(values.get("phone")));
                    rider.setRiderType(parseRiderType(values.get("riderType")));
                    rider.setIdCard(blankToNull(values.get("idCard")));
                    rider.setDriverLicense(blankToNull(values.get("driverLicense")));
                    rider.setHealthCertNo(blankToNull(values.get("healthCertNo")));
                    rider.setQualificationExpireDate(parseDate(values.get("qualificationExpireDate"), rowNo, errors));
                    rider.setEntryDate(parseDate(values.get("entryDate"), rowNo, errors));
                    rider.setVehicleNo(blankToNull(values.get("vehicleNo")));
                    rider.setSettleMethod(parseSettleMethod(values.get("settleMethod")));
                    rider.setRemark(blankToNull(values.get("remark")));
                    rider.setPlatformRiderId(blankToNull(values.get("platformRiderId")));
                    rider.setChannelId(resolveChannelId(values.get("channelName"), rowNo, errors));
                    rider.setDeptId(resolveDeptId(values.get("deptName")));

                    validateByType(rider, true);

                    String riderNo = blankToNull(values.get("riderNo"));
                    rider.setRiderNo(riderNo == null ? nextCode() : riderNo);
                    assertRiderNoUnique(rider.getRiderNo(), null);
                    rider.setTenantId(currentTenantId());
                    rider.setStatus(0);
                    rider.setVerifyStatus(0);
                    rider.setRatingScore(new BigDecimal("5.00"));
                    rider.setTotalOrders(0);
                    rider.setTodayOrders(0);
                    rider.setPunctualRate(new BigDecimal("100.00"));
                    riderMapper.insert(rider);
                    success++;
                } catch (Exception ex) {
                    log.warn("配送员导入第{}行失败", rowNo, ex);
                    errors.add("第" + rowNo + "行：" + ex.getMessage());
                }
            }
        } catch (IOException e) {
            throw new BusinessException("Excel 解析失败：" + e.getMessage());
        }
        log.info("配送员导入完成: total={}, success={}, failure={}", total, success, errors.size());
        return importResult(total, success, errors);
    }

    private Map<String, Object> importResult(int total, int success, List<String> errors) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failure", errors.size());
        result.put("errors", errors);
        return result;
    }

    /** 模板表头 → 字段名（与模板列名保持一致，宽容匹配） */
    private String importField(String header) {
        if (header == null) {
            return null;
        }
        String h = header.trim();
        if (h.isEmpty() || h.startsWith("导入结果")) {
            return null;
        }
        if (h.contains("编号")) return "riderNo";
        if (h.contains("姓名")) return "realName";
        if (h.contains("手机")) return "phone";
        if (h.contains("类型")) return "riderType";
        if (h.contains("渠道")) return "channelName";
        if (h.contains("平台骑手")) return "platformRiderId";
        if (h.contains("身份证")) return "idCard";
        if (h.contains("驾驶证")) return "driverLicense";
        if (h.contains("健康证")) return "healthCertNo";
        if (h.contains("资质") || h.contains("有效期")) return "qualificationExpireDate";
        if (h.contains("入职")) return "entryDate";
        if (h.contains("部门")) return "deptName";
        if (h.contains("车牌")) return "vehicleNo";
        if (h.contains("结算")) return "settleMethod";
        if (h.contains("备注")) return "remark";
        return null;
    }

    private Integer parseRiderType(String text) {
        if (!hasText(text)) {
            throw new BusinessException("配送员类型不能为空（企业员工/众包兼职/外部平台配送员/社会车辆司机）");
        }
        String value = text.trim();
        for (RiderTypeEnum type : RiderTypeEnum.values()) {
            if (type.getDescription().equals(value) || String.valueOf(type.getValue()).equals(value)) {
                return type.getValue();
            }
        }
        throw new BusinessException("配送员类型「" + value + "」无法识别");
    }

    private Integer parseSettleMethod(String text) {
        if (!hasText(text)) {
            return null;
        }
        String value = text.trim();
        for (Map.Entry<Integer, String> entry : SETTLE_TEXT.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("结算方式「" + value + "」无法识别");
        }
    }

    private LocalDate parseDate(String text, int rowNo, List<String> errors) {
        if (!hasText(text)) {
            return null;
        }
        String value = text.trim().replace('/', '-').replace(".", "-");
        try {
            if (value.length() == 8 && value.chars().allMatch(Character::isDigit)) {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
            return LocalDate.parse(value);
        } catch (Exception e) {
            errors.add("第" + rowNo + "行：日期「" + text + "」格式不正确（应为 yyyy-MM-dd）");
            throw new BusinessException("日期格式不正确：" + text);
        }
    }

    private Long resolveChannelId(String channelName, int rowNo, List<String> errors) {
        if (!hasText(channelName)) {
            return null;
        }
        List<DmsChannel> channels = channelMapper.selectList(new LambdaQueryWrapper<DmsChannel>()
                .eq(DmsChannel::getChannelName, channelName.trim())
                .last("LIMIT 1"));
        if (channels.isEmpty()) {
            errors.add("第" + rowNo + "行：渠道「" + channelName + "」不存在，请先在《渠道管理》维护");
            throw new BusinessException("渠道不存在：" + channelName);
        }
        return channels.get(0).getId();
    }

    private Long resolveDeptId(String deptName) {
        if (!hasText(deptName)) {
            return null;
        }
        List<SysDept> depts = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDeptName, deptName.trim())
                .last("LIMIT 1"));
        return depts.isEmpty() ? null : depts.get(0).getId();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String blankToNull(String value) {
        return trim(value);
    }
}
