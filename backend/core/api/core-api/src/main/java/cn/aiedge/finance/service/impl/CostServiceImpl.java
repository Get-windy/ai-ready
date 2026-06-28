package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.ProductCostStandard;
import cn.aiedge.finance.mapper.ProductCostStandardMapper;
import cn.aiedge.finance.service.ICostService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CostServiceImpl implements ICostService {

    private final ProductCostStandardMapper productCostStandardMapper;

    private Long getTenantId() {
        Object tid = StpUtil.getSession().get("tenantId");
        return tid instanceof Number ? ((Number) tid).longValue() : 0L;
    }

    @Override
    public Page<ProductCostStandard> pageCost(Integer pageNum, Integer pageSize, String productName, String batchNo) {
        Long tenantId = getTenantId();
        LambdaQueryWrapper<ProductCostStandard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductCostStandard::getTenantId, tenantId);
        wrapper.eq(ProductCostStandard::getDeleted, 0);

        if (productName != null && !productName.isEmpty()) {
            // 由于 erp_product_cost_standard 表没有 productName 字段，
            // 通过 productId 关联查询不在此处实现，简化处理
            wrapper.like(ProductCostStandard::getRemark, productName);
        }

        wrapper.orderByDesc(ProductCostStandard::getCreateTime);
        return productCostStandardMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public Map<String, Object> getStats() {
        Long tenantId = getTenantId();
        Map<String, Object> stats = productCostStandardMapper.selectStats(tenantId);
        if (stats == null) {
            stats = new HashMap<>();
            stats.put("materialCost", 0);
            stats.put("laborCost", 0);
            stats.put("overheadCost", 0);
            stats.put("totalCost", 0);
        }
        return stats;
    }
}
