package cn.aiedge.erp.party.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyCreditMapper;
import cn.aiedge.erp.party.mapper.PartyMapper;
import cn.aiedge.erp.party.service.PartyCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 往来单位信用服务实现（详见 {@link PartyCreditService} 的归属说明）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartyCreditServiceImpl implements PartyCreditService {

    /** 额度为 0 时视为「未配置信用管控」，给一个足够大的可用额度（与原 CRM 实现口径一致） */
    private static final BigDecimal UNLIMITED_CREDIT = new BigDecimal("999999999.99");

    /** 预警阈值（使用率 %） */
    private static final BigDecimal WARN_USAGE_RATE = BigDecimal.valueOf(80);
    /** 超额阈值（使用率 %） */
    private static final BigDecimal OVER_USAGE_RATE = BigDecimal.valueOf(100);

    private final PartyMapper partyMapper;
    private final PartyCreditMapper partyCreditMapper;

    @Override
    public BigDecimal getCreditLimit(Long partyId) {
        Party party = partyMapper.selectById(partyId);
        if (party == null || party.getCreditLimit() == null) {
            return BigDecimal.ZERO;
        }
        return party.getCreditLimit();
    }

    @Override
    public BigDecimal getCurrentDebt(Long partyId) {
        Party party = partyMapper.selectById(partyId);
        if (party == null || party.getCurrentDebt() == null) {
            return BigDecimal.ZERO;
        }
        return party.getCurrentDebt();
    }

    @Override
    public BigDecimal getAvailableCredit(Long partyId) {
        BigDecimal creditLimit = getCreditLimit(partyId);
        if (creditLimit.compareTo(BigDecimal.ZERO) == 0) {
            return UNLIMITED_CREDIT;
        }
        return creditLimit.subtract(getCurrentDebt(partyId)).max(BigDecimal.ZERO);
    }

    @Override
    public boolean checkCreditAvailable(Long partyId, BigDecimal newAmount) {
        BigDecimal amount = newAmount == null ? BigDecimal.ZERO : newAmount;
        return getAvailableCredit(partyId).compareTo(amount) >= 0;
    }

    @Override
    public Map<String, Object> getCreditStatus(Long partyId) {
        Party party = partyMapper.selectById(partyId);
        if (party == null) {
            throw BusinessException.notFound("往来单位不存在");
        }

        BigDecimal creditLimit = party.getCreditLimit() != null ? party.getCreditLimit() : BigDecimal.ZERO;
        BigDecimal debt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;

        Map<String, Object> status = new HashMap<>();
        status.put("partyId", partyId);
        status.put("partyName", party.getPartyName());
        status.put("creditLimit", creditLimit);
        status.put("currentDebt", debt);
        status.put("availableCredit", getAvailableCredit(partyId));

        BigDecimal usageRate = BigDecimal.ZERO;
        if (creditLimit.compareTo(BigDecimal.ZERO) > 0) {
            usageRate = debt.divide(creditLimit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        status.put("usageRate", usageRate);

        // 顺序必须是「先 overdue 后 warning」：≥100 也满足 ≥80，先判 80 会让 overdue 永不命中
        String creditStatus = "normal";
        if (usageRate.compareTo(OVER_USAGE_RATE) >= 0) {
            creditStatus = "overdue";
        } else if (usageRate.compareTo(WARN_USAGE_RATE) >= 0) {
            creditStatus = "warning";
        }
        status.put("creditStatus", creditStatus);
        return status;
    }

    @Override
    public List<Map<String, Object>> getCreditWarningList() {
        List<Map<String, Object>> warningList = new ArrayList<>();
        for (Party party : listPartiesWithCreditLimit()) {
            BigDecimal usageRate = usageRateOf(party);
            if (usageRate.compareTo(WARN_USAGE_RATE) >= 0 && usageRate.compareTo(OVER_USAGE_RATE) < 0) {
                warningList.add(creditRow(party, usageRate));
            }
        }
        warningList.sort(Comparator.comparing((Map<String, Object> m) -> (BigDecimal) m.get("usageRate")).reversed());
        return warningList;
    }

    @Override
    public List<Map<String, Object>> getOverCreditList() {
        List<Map<String, Object>> overCreditList = new ArrayList<>();
        for (Party party : listPartiesWithCreditLimit()) {
            BigDecimal creditLimit = party.getCreditLimit();
            BigDecimal debt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
            if (debt.compareTo(creditLimit) > 0) {
                Map<String, Object> row = creditRow(party, usageRateOf(party));
                row.put("overAmount", debt.subtract(creditLimit));
                overCreditList.add(row);
            }
        }
        overCreditList.sort(Comparator.comparing((Map<String, Object> m) -> (BigDecimal) m.get("overAmount")).reversed());
        return overCreditList;
    }

    @Override
    public Map<String, Object> getCreditStatistics() {
        BigDecimal totalCreditLimit = BigDecimal.ZERO;
        BigDecimal totalDebt = BigDecimal.ZERO;
        int normalCount = 0;
        int warningCount = 0;
        int overCreditCount = 0;

        List<Party> parties = listPartiesWithCreditLimit();
        for (Party party : parties) {
            BigDecimal creditLimit = party.getCreditLimit();
            BigDecimal debt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
            totalCreditLimit = totalCreditLimit.add(creditLimit);
            totalDebt = totalDebt.add(debt);

            BigDecimal usageRate = usageRateOf(party);
            if (usageRate.compareTo(OVER_USAGE_RATE) >= 0) {
                overCreditCount++;
            } else if (usageRate.compareTo(WARN_USAGE_RATE) >= 0) {
                warningCount++;
            } else {
                normalCount++;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCreditLimit", totalCreditLimit);
        stats.put("totalDebt", totalDebt);
        stats.put("totalAvailableCredit", totalCreditLimit.subtract(totalDebt));
        stats.put("partyCount", parties.size());
        stats.put("normalCount", normalCount);
        stats.put("warningCount", warningCount);
        stats.put("overCreditCount", overCreditCount);
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalcPartyDebt(Long partyId) {
        BigDecimal debt = partyCreditMapper.sumRemainingReceivable(partyId);
        partyCreditMapper.updateCurrentDebt(partyId, debt == null ? BigDecimal.ZERO : debt);
    }

    /**
     * 每日重算所有配置了信用额度的往来单位欠款。
     *
     * <p>与迁走前那份 CRM 实现的关键差别：{@code calculateTotalDebt} 不再是恒返回 0 的桩
     * —— 旧实现配合这个定时任务会**每天凌晨把所有客户欠款清零**。这里按应收余额真实汇总。</p>
     *
     * <p>跑在无会话上下文的定时任务里，涉及的两条 SQL
     * （{@code sumRemainingReceivable} / {@code updateCurrentDebt}）都标了
     * {@code @InterceptorIgnore(tenantLine = "true")}：本任务要跨全部租户重算，
     * 而这两条语句要么只看一个数值列、要么按全局唯一的 party id 更新，不需要租户条件。</p>
     */
    @Override
    @Scheduled(cron = "0 0 1 * * ?")
    public void recalcAllPartyDebt() {
        List<Long> partyIds = partyCreditMapper.selectPartiesWithCreditLimit();
        int ok = 0;
        for (Long partyId : partyIds) {
            try {
                recalcPartyDebt(partyId);
                ok++;
            } catch (Exception e) {
                log.error("重算往来单位欠款失败: partyId={}", partyId, e);
            }
        }
        log.info("往来单位欠款重算完成：扫描 {} 个，成功 {} 个", partyIds.size(), ok);
    }

    // ── 内部工具 ────────────────────────────────────────────────

    private List<Party> listPartiesWithCreditLimit() {
        List<Long> ids = partyCreditMapper.selectPartiesWithCreditLimit();
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return partyMapper.selectBatchIds(ids);
    }

    private BigDecimal usageRateOf(Party party) {
        BigDecimal creditLimit = party.getCreditLimit();
        if (creditLimit == null || creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal debt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
        return debt.divide(creditLimit, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private Map<String, Object> creditRow(Party party, BigDecimal usageRate) {
        BigDecimal creditLimit = party.getCreditLimit();
        BigDecimal debt = party.getCurrentDebt() != null ? party.getCurrentDebt() : BigDecimal.ZERO;
        Map<String, Object> row = new HashMap<>();
        row.put("partyId", party.getId());
        row.put("partyName", party.getPartyName());
        row.put("creditLimit", creditLimit);
        row.put("currentDebt", debt);
        row.put("availableCredit", creditLimit.subtract(debt));
        row.put("usageRate", usageRate);
        return row;
    }
}
