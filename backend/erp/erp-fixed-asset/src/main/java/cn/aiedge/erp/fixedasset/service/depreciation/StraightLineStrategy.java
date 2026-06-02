package cn.aiedge.erp.fixedasset.service.depreciation;

import cn.aiedge.erp.fixedasset.model.FixedAsset;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 直线法折旧策略
 * 月折旧额 = (原值 - 残值) / 使用年限(月)
 */
@Component
public class StraightLineStrategy implements DepreciationStrategy {

    @Override
    public Map<String, BigDecimal> calculateDepreciation(FixedAsset asset) {
        Map<String, BigDecimal> result = new HashMap<>();

        if (asset.getOriginalValue() == null || asset.getUsefulLife() == null || asset.getUsefulLife() <= 0) {
            result.put("periodAmount", BigDecimal.ZERO);
            result.put("accumulatedDepreciation", asset.getAccumulatedDepreciation() != null ? asset.getAccumulatedDepreciation() : BigDecimal.ZERO);
            result.put("netValue", asset.getOriginalValue() != null ? asset.getOriginalValue() : BigDecimal.ZERO);
            return result;
        }

        BigDecimal originalValue = asset.getOriginalValue();
        BigDecimal salvageValue = asset.getSalvageValue() != null ? asset.getSalvageValue() : BigDecimal.ZERO;
        BigDecimal depreciableBase = originalValue.subtract(salvageValue);

        if (depreciableBase.compareTo(BigDecimal.ZERO) <= 0) {
            result.put("periodAmount", BigDecimal.ZERO);
            result.put("accumulatedDepreciation", asset.getAccumulatedDepreciation() != null ? asset.getAccumulatedDepreciation() : BigDecimal.ZERO);
            result.put("netValue", originalValue);
            return result;
        }

        BigDecimal usefulLife = BigDecimal.valueOf(asset.getUsefulLife());
        BigDecimal periodAmount = depreciableBase.divide(usefulLife, 2, RoundingMode.HALF_UP);

        BigDecimal currentAccumulated = asset.getAccumulatedDepreciation() != null ? asset.getAccumulatedDepreciation() : BigDecimal.ZERO;
        BigDecimal newAccumulated = currentAccumulated.add(periodAmount);

        // 确保累计折旧不超过折旧基数
        if (newAccumulated.compareTo(depreciableBase) > 0) {
            periodAmount = depreciableBase.subtract(currentAccumulated);
            if (periodAmount.compareTo(BigDecimal.ZERO) < 0) {
                periodAmount = BigDecimal.ZERO;
            }
            newAccumulated = currentAccumulated.add(periodAmount);
        }

        BigDecimal netValue = originalValue.subtract(newAccumulated);
        if (netValue.compareTo(BigDecimal.ZERO) < 0) {
            netValue = BigDecimal.ZERO;
        }

        result.put("periodAmount", periodAmount);
        result.put("accumulatedDepreciation", newAccumulated);
        result.put("netValue", netValue);

        return result;
    }
}
