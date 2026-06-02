package cn.aiedge.erp.fixedasset.service.depreciation;

import cn.aiedge.erp.fixedasset.model.FixedAsset;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 双倍余额递减法折旧策略
 * 年折旧率 = 2 / 预计使用年限
 * 月折旧额 = 净值 * 年折旧率 / 12
 * 最后两年改用直线法
 */
@Component
public class DoubleDecliningStrategy implements DepreciationStrategy {

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
        BigDecimal currentNetValue = asset.getNetValue() != null ? asset.getNetValue() : originalValue;
        BigDecimal currentAccumulated = asset.getAccumulatedDepreciation() != null ? asset.getAccumulatedDepreciation() : BigDecimal.ZERO;

        int totalMonths = asset.getUsefulLife();

        // 计算已折旧月份数（估算）
        BigDecimal estimatedMonthlyDepreciation = originalValue.subtract(salvageValue)
            .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
        int monthsElapsed = estimatedMonthlyDepreciation.compareTo(BigDecimal.ZERO) > 0
            ? currentAccumulated.divide(estimatedMonthlyDepreciation, 0, RoundingMode.HALF_UP).intValue()
            : 0;
        int remainingMonths = totalMonths - monthsElapsed;

        BigDecimal periodAmount;

        // 最后两年（24个月）改用直线法
        if (remainingMonths <= 24 && remainingMonths > 0) {
            // 直线法: 当前净值减去残值 / 剩余月份
            BigDecimal remainingDepreciable = currentNetValue.subtract(salvageValue);
            if (remainingDepreciable.compareTo(BigDecimal.ZERO) <= 0) {
                periodAmount = BigDecimal.ZERO;
            } else {
                periodAmount = remainingDepreciable.divide(BigDecimal.valueOf(remainingMonths), 2, RoundingMode.HALF_UP);
            }
        } else {
            // 双倍余额递减法: 月折旧额 = 净值 * 2 / 总月份
            BigDecimal annualRate = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(totalMonths / 12.0), 4, RoundingMode.HALF_UP);
            BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
            periodAmount = currentNetValue.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal newAccumulated = currentAccumulated.add(periodAmount);
        BigDecimal totalDepreciable = originalValue.subtract(salvageValue);

        // 确保累计折旧不超过折旧基数
        if (newAccumulated.compareTo(totalDepreciable) > 0) {
            periodAmount = totalDepreciable.subtract(currentAccumulated);
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
