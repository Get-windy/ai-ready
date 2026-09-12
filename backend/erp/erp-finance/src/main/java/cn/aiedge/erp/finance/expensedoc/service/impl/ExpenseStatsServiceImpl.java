package cn.aiedge.erp.finance.expensedoc.service.impl;

import cn.aiedge.erp.finance.expensedoc.dto.ExpenseDocQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsAggVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsRowVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsSummaryVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseStatsTrendVO;
import cn.aiedge.erp.finance.expensedoc.mapper.ExpenseStatsMapper;
import cn.aiedge.erp.finance.expensedoc.service.ExpenseStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 费用统计 Service 实现
 *
 * <p>占比、单均金额等派生指标统一在后端计算（口径唯一），前端只做展示。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseStatsServiceImpl implements ExpenseStatsService {

    /** 占比精度：2 位小数（%） */
    private static final int RATIO_SCALE = 2;
    /** 金额精度：2 位小数 */
    private static final int AMOUNT_SCALE = 2;

    private final ExpenseStatsMapper expenseStatsMapper;

    @Override
    public ExpenseStatsSummaryVO getSummary(ExpenseDocQuery query) {
        ExpenseDocQuery q = query != null ? query : new ExpenseDocQuery();
        ExpenseStatsAggVO agg = expenseStatsMapper.aggregate(q);
        List<ExpenseStatsRowVO> deptRows = expenseStatsMapper.listByDepartment(q);
        List<ExpenseStatsRowVO> typeRows = expenseStatsMapper.listByType(q);
        List<ExpenseStatsTrendVO> trend = expenseStatsMapper.listMonthlyTrend(q);

        ExpenseStatsSummaryVO vo = new ExpenseStatsSummaryVO();
        if (agg != null) {
            vo.setExpenseCount(nvlInt(agg.getExpenseCount()));
            vo.setTotalAmount(nvl(agg.getTotalAmount()));
            vo.setApprovedAmount(nvl(agg.getApprovedAmount()));
            vo.setPendingAmount(nvl(agg.getPendingAmount()));
            vo.setRejectedAmount(nvl(agg.getRejectedAmount()));
            vo.setDraftAmount(nvl(agg.getDraftAmount()));
            vo.setPaidAmount(nvl(agg.getPaidAmount()));
            vo.setUnpaidAmount(nvl(agg.getUnpaidAmount()));
            vo.setPartnerAmount(nvl(agg.getPartnerAmount()));
            vo.setInternalAmount(nvl(agg.getInternalAmount()));
        }
        // 费用笔数：与分组维度同口径（明细级）
        Integer itemCount = expenseStatsMapper.countItems(q);
        vo.setItemCount(nvlInt(itemCount));
        // 平均单额 = 费用总额 / 单据数
        if (vo.getExpenseCount() > 0) {
            vo.setAverageAmount(vo.getTotalAmount()
                    .divide(BigDecimal.valueOf(vo.getExpenseCount()), AMOUNT_SCALE, RoundingMode.HALF_UP));
        } else {
            vo.setAverageAmount(BigDecimal.ZERO);
        }

        // 分组映射（名称 → 金额），空值按 0 处理；同时回填占比
        BigDecimal total = vo.getTotalAmount();
        Map<String, BigDecimal> byType = new LinkedHashMap<>();
        for (ExpenseStatsRowVO row : typeRows) {
            BigDecimal amount = nvl(row.getTotalAmount());
            row.setRatio(ratioOf(amount, total));
            row.setAvgAmount(avgOf(amount, nvlInt(row.getDocCount())));
            byType.put(row.getGroupKey(), amount);
        }
        Map<String, BigDecimal> byDept = new LinkedHashMap<>();
        for (ExpenseStatsRowVO row : deptRows) {
            BigDecimal amount = nvl(row.getTotalAmount());
            row.setRatio(ratioOf(amount, total));
            row.setAvgAmount(avgOf(amount, nvlInt(row.getDocCount())));
            byDept.put(row.getGroupKey(), amount);
        }
        vo.setByType(byType);
        vo.setByDepartment(byDept);
        for (ExpenseStatsTrendVO t : trend) {
            t.setTotalAmount(nvl(t.getTotalAmount()));
            t.setApprovedAmount(nvl(t.getApprovedAmount()));
            t.setPaidAmount(nvl(t.getPaidAmount()));
            t.setDocCount(nvlInt(t.getDocCount()));
        }
        vo.setMonthlyTrend(trend);
        return vo;
    }

    @Override
    public List<ExpenseStatsRowVO> listByDepartment(ExpenseDocQuery query) {
        ExpenseDocQuery q = query != null ? query : new ExpenseDocQuery();
        List<ExpenseStatsRowVO> rows = expenseStatsMapper.listByDepartment(q);
        fillDerived(rows, expenseStatsMapper.aggregate(q));
        return rows;
    }

    @Override
    public List<ExpenseStatsRowVO> listByType(ExpenseDocQuery query) {
        ExpenseDocQuery q = query != null ? query : new ExpenseDocQuery();
        List<ExpenseStatsRowVO> rows = expenseStatsMapper.listByType(q);
        fillDerived(rows, expenseStatsMapper.aggregate(q));
        return rows;
    }

    @Override
    public List<ExpenseStatsTrendVO> listMonthlyTrend(ExpenseDocQuery query) {
        ExpenseDocQuery q = query != null ? query : new ExpenseDocQuery();
        List<ExpenseStatsTrendVO> rows = expenseStatsMapper.listMonthlyTrend(q);
        for (ExpenseStatsTrendVO row : rows) {
            row.setTotalAmount(nvl(row.getTotalAmount()));
            row.setApprovedAmount(nvl(row.getApprovedAmount()));
            row.setPaidAmount(nvl(row.getPaidAmount()));
            row.setDocCount(nvlInt(row.getDocCount()));
        }
        return rows;
    }

    /** 派生指标：占比 / 单均金额（占分母用单据级「费用总额」，与卡片口径一致） */
    private void fillDerived(List<ExpenseStatsRowVO> rows, ExpenseStatsAggVO agg) {
        BigDecimal total = agg != null ? nvl(agg.getTotalAmount()) : BigDecimal.ZERO;
        for (ExpenseStatsRowVO row : rows) {
            BigDecimal amount = nvl(row.getTotalAmount());
            row.setApprovedAmount(nvl(row.getApprovedAmount()));
            row.setPendingAmount(nvl(row.getPendingAmount()));
            row.setRejectedAmount(nvl(row.getRejectedAmount()));
            row.setPaidAmount(nvl(row.getPaidAmount()));
            row.setRatio(ratioOf(amount, total));
            row.setAvgAmount(avgOf(amount, nvlInt(row.getDocCount())));
        }
    }

    private BigDecimal ratioOf(BigDecimal amount, BigDecimal total) {
        if (amount == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, RATIO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal avgOf(BigDecimal amount, int count) {
        if (amount == null || count <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(BigDecimal.valueOf(count), AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private int nvlInt(Integer value) {
        return value != null ? value : 0;
    }
}
