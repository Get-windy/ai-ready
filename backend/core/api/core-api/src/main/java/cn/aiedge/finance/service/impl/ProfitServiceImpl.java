package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.ProfitAnalysis;
import cn.aiedge.finance.mapper.ProfitAnalysisMapper;
import cn.aiedge.finance.service.IProfitService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfitServiceImpl implements IProfitService {

    private final ProfitAnalysisMapper profitAnalysisMapper;

    private Long getTenantId() {
        Object tid = StpUtil.getSession().get("tenantId");
        return tid instanceof Number ? ((Number) tid).longValue() : 0L;
    }

    @Override
    public Page<ProfitAnalysis> pageProfit(Integer pageNum, Integer pageSize, String productName, String startDate, String endDate) {
        Long tenantId = getTenantId();
        LambdaQueryWrapper<ProfitAnalysis> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProfitAnalysis::getTenantId, tenantId);
        wrapper.eq(ProfitAnalysis::getDeleted, 0);

        if (productName != null && !productName.isEmpty()) {
            wrapper.like(ProfitAnalysis::getProductName, productName);
        }
        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge(ProfitAnalysis::getAnalysisDate, startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le(ProfitAnalysis::getAnalysisDate, endDate);
        }

        wrapper.orderByDesc(ProfitAnalysis::getAnalysisDate);
        return profitAnalysisMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Map<String, Object> getStats() {
        Long tenantId = getTenantId();
        Map<String, Object> dbStats = profitAnalysisMapper.selectStats(tenantId);

        Map<String, Object> stats = new HashMap<>();
        if (dbStats == null) {
            stats.put("totalRevenue", 0);
            stats.put("totalProfit", 0);
            stats.put("avgMargin", 0);
            return stats;
        }

        Object revenueObj = dbStats.get("totalRevenue");
        Object profitObj = dbStats.get("totalProfit");
        BigDecimal totalRevenue = revenueObj instanceof Number ? BigDecimal.valueOf(((Number) revenueObj).doubleValue()) : BigDecimal.ZERO;
        BigDecimal totalProfit = profitObj instanceof Number ? BigDecimal.valueOf(((Number) profitObj).doubleValue()) : BigDecimal.ZERO;
        BigDecimal avgMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        stats.put("totalRevenue", totalRevenue);
        stats.put("totalProfit", totalProfit);
        stats.put("avgMargin", avgMargin);
        return stats;
    }
}
