package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.erp.marketing.entity.MemberConfig;
import cn.aiedge.erp.marketing.entity.PointsBatch;
import cn.aiedge.erp.marketing.entity.PointsJournal;
import cn.aiedge.erp.marketing.mapper.PointsBatchMapper;
import cn.aiedge.erp.marketing.mapper.PointsJournalMapper;
import cn.aiedge.erp.marketing.service.MemberConfigService;
import cn.aiedge.erp.marketing.service.PointsLedgerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointsLedgerServiceImpl implements PointsLedgerService {

    private final PointsBatchMapper batchMapper;
    private final PointsJournalMapper journalMapper;
    private final MemberConfigService memberConfigService;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private Long tenantId() {
        Long t = SecurityUtils.getCurrentTenantId();
        return t == null ? 1L : t;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? ZERO : v; }

    @Override
    public List<PointsBatch> listBatches(String memberCardNo) {
        return batchMapper.selectList(new LambdaQueryWrapper<PointsBatch>()
                .eq(PointsBatch::getMemberCardNo, memberCardNo)
                .orderByAsc(PointsBatch::getExpireTime)
                .orderByAsc(PointsBatch::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long earn(String memberCardNo, Long partnerId, BigDecimal points, String source, String billNo) {
        if (memberCardNo == null || points == null || points.compareTo(ZERO) <= 0) return null;
        MemberConfig cfg = memberConfigService.getConfig();
        Integer months = cfg == null ? null : cfg.getPointsValidMonths();

        LocalDateTime now = LocalDateTime.now();
        PointsBatch batch = new PointsBatch()
                .setTenantId(tenantId())
                .setMemberCardNo(memberCardNo)
                .setPartnerId(partnerId)
                .setEarnedPoints(points)
                .setRemainingPoints(points)
                .setEarnedTime(now)
                // 0 / NULL = 永不过期
                .setExpireTime(months == null || months <= 0 ? null : now.plusMonths(months))
                .setSource(source == null ? "ADJUST" : source)
                .setSourceBillNo(billNo)
                .setStatus(PointsBatch.ACTIVE)
                .setDeleted(0)
                .setCreateTime(now)
                .setUpdateTime(now);
        batchMapper.insert(batch);

        writeJournal(memberCardNo, partnerId, PointsJournal.EARN, points, batch.getId(), billNo,
                "积分获得（有效期 " + (months == null || months <= 0 ? "永久" : months + " 个月") + "）");
        return batch.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal use(String memberCardNo, BigDecimal points, String billNo) {
        if (memberCardNo == null || points == null || points.compareTo(ZERO) <= 0) return ZERO;
        BigDecimal remainingToUse = points;
        BigDecimal used = ZERO;

        // FIFO：先到期的先用（expire_time 升序；永久有效的排最后）
        for (PointsBatch b : availableBatches(memberCardNo)) {
            if (remainingToUse.compareTo(ZERO) <= 0) break;
            BigDecimal avail = nz(b.getRemainingPoints());
            if (avail.compareTo(ZERO) <= 0) continue;
            BigDecimal cut = avail.min(remainingToUse);
            BigDecimal left = avail.subtract(cut);
            b.setRemainingPoints(left);
            b.setStatus(left.compareTo(ZERO) <= 0 ? PointsBatch.EXHAUSTED : PointsBatch.ACTIVE);
            b.setUpdateTime(LocalDateTime.now());
            batchMapper.updateById(b);
            remainingToUse = remainingToUse.subtract(cut);
            used = used.add(cut);
        }

        if (used.compareTo(ZERO) > 0) {
            writeJournal(memberCardNo, null, PointsJournal.USE, used.negate(), null, billNo, "积分使用（FIFO 扣减）");
        }
        return used;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal adjust(String memberCardNo, Long partnerId, BigDecimal points, String remark) {
        if (points == null || points.compareTo(ZERO) == 0) return ZERO;
        if (points.compareTo(ZERO) > 0) {
            earn(memberCardNo, partnerId, points, "ADJUST", null);
            return points;
        }
        return use(memberCardNo, points.negate(), null).negate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, BigDecimal> expireDue(LocalDate asOf) {
        LocalDate date = asOf == null ? LocalDate.now() : asOf;
        LocalDateTime cutoff = date.atTime(23, 59, 59);
        List<PointsBatch> due = batchMapper.selectList(new LambdaQueryWrapper<PointsBatch>()
                .eq(PointsBatch::getStatus, PointsBatch.ACTIVE)
                .isNotNull(PointsBatch::getExpireTime)
                .le(PointsBatch::getExpireTime, cutoff)
                .gt(PointsBatch::getRemainingPoints, ZERO));

        Map<String, BigDecimal> out = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (PointsBatch b : due) {
            BigDecimal expired = nz(b.getRemainingPoints());
            b.setRemainingPoints(ZERO);
            b.setStatus(PointsBatch.EXPIRED);
            b.setUpdateTime(now);
            batchMapper.updateById(b);
            writeJournal(b.getMemberCardNo(), b.getPartnerId(), PointsJournal.EXPIRE, expired.negate(),
                    b.getId(), null, "积分到期扣减（批次到期 " + b.getExpireTime() + "）");
            out.merge(b.getMemberCardNo(), expired, BigDecimal::add);
        }
        if (!out.isEmpty()) {
            log.info("[积分台账] 过期处理完成：{} 个会员共 {} 分", out.size(),
                    out.values().stream().reduce(ZERO, BigDecimal::add));
        }
        return out;
    }

    @Override
    public List<PointsBatch> expiringSoon(int days) {
        int d = days <= 0 ? 30 : days;
        LocalDateTime now = LocalDateTime.now();
        return batchMapper.selectList(new LambdaQueryWrapper<PointsBatch>()
                .eq(PointsBatch::getStatus, PointsBatch.ACTIVE)
                .isNotNull(PointsBatch::getExpireTime)
                .gt(PointsBatch::getRemainingPoints, ZERO)
                .between(PointsBatch::getExpireTime, now, now.plusDays(d))
                .orderByAsc(PointsBatch::getExpireTime));
    }

    @Override
    public BigDecimal available(String memberCardNo) {
        return availableBatches(memberCardNo).stream()
                .map(b -> nz(b.getRemainingPoints()))
                .reduce(ZERO, BigDecimal::add);
    }

    /** 可扣减批次（FIFO 顺序；永久有效的排最后） */
    private List<PointsBatch> availableBatches(String memberCardNo) {
        List<PointsBatch> list = batchMapper.selectList(new LambdaQueryWrapper<PointsBatch>()
                .eq(PointsBatch::getMemberCardNo, memberCardNo)
                .eq(PointsBatch::getStatus, PointsBatch.ACTIVE)
                .gt(PointsBatch::getRemainingPoints, ZERO));
        list.sort((a, b) -> {
            boolean an = a.getExpireTime() == null;
            boolean bn = b.getExpireTime() == null;
            if (an && bn) return 0;
            if (an) return 1;
            if (bn) return -1;
            return a.getExpireTime().compareTo(b.getExpireTime());
        });
        return list;
    }

    private void writeJournal(String cardNo, Long partnerId, String type, BigDecimal change,
                              Long batchId, String billNo, String remark) {
        journalMapper.insert(new PointsJournal()
                .setTenantId(tenantId())
                .setMemberCardNo(cardNo)
                .setPartnerId(partnerId)
                .setChangeType(type)
                .setChangePoints(change)
                .setBalanceAfter(available(cardNo))
                .setBatchId(batchId)
                .setSourceBillNo(billNo)
                .setRemark(remark)
                .setDeleted(0)
                .setCreateTime(LocalDateTime.now()));
    }
}
