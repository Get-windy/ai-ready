package cn.aiedge.erp.fixedasset.service.depreciation;

import cn.aiedge.erp.fixedasset.model.FixedAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 折旧策略上下文
 */
@Component
@RequiredArgsConstructor
public class DepreciationContext {

    private final StraightLineStrategy straightLineStrategy;
    private final DoubleDecliningStrategy doubleDecliningStrategy;
    private final SumOfYearsDigitsStrategy sumOfYearsDigitsStrategy;

    /**
     * 根据资产的折旧方法选择对应策略并计算折旧
     */
    public Map<String, BigDecimal> calculateDepreciation(FixedAsset asset) {
        if (asset.getDepreciationMethod() == null) {
            throw new IllegalArgumentException("折旧方法不能为空");
        }

        DepreciationStrategy strategy = getStrategy(asset.getDepreciationMethod());
        return strategy.calculateDepreciation(asset);
    }

    private DepreciationStrategy getStrategy(String method) {
        return switch (method) {
            case "straight_line" -> straightLineStrategy;
            case "double_declining" -> doubleDecliningStrategy;
            case "sum_of_years" -> sumOfYearsDigitsStrategy;
            default -> throw new IllegalArgumentException("不支持的折旧方法: " + method);
        };
    }
}
