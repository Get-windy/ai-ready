package cn.aiedge.erp.fixedasset.service.depreciation;

import cn.aiedge.erp.fixedasset.model.FixedAsset;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * 年数总和法折旧策略
 * 年折旧率 = 尚可使用年数 / 年数总和
 * 月折旧额 = (原值 - 残值) * 月折旧率
 */
@Component
public class SumOfYearsDigitsStrategy implements DepreciationStrategy {

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

        int totalMonths = asset.getUsefulLife();
        // 年数总和 S = n * (n + 1) / 2 (以年为单位)
        int totalYears = totalMonths / 12;
        if (totalYears <= 0) {
            totalYears = 1;
        }
        int sumOfYears = totalYears * (totalYears + 1) / 2;

        // 估算已使用年数
        BigDecimal currentAccumulated = asset.getAccumulatedDepreciation() != null ? asset.getAccumulatedDepreciation() : BigDecimal.ZERO;
        BigDecimal monthlySL = depreciableBase.divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP);
        int monthsElapsed = monthlySL.compareTo(BigDecimal.ZERO) > 0
            ? currentAccumulated.divide(monthlySL, 0, RoundingMode.HALF_UP).intValue()
            : 0;
        int yearsElapsed = monthsElapsed / 12;
        int remainingYears = totalYears - yearsElapsed;
        if (remainingYears <= 0) {
            remainingYears = 1;
        }

        // 当年折旧率 = 尚可使用年数 / 年数总和
        BigDecimal yearlyRate = BigDecimal.valueOf(remainingYears).divide(BigDecimal.valueOf(sumOfYears), 4, RoundingMode.HALF_UP);
        // 月折旧率
        BigDecimal monthlyRate = yearlyRate.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);

        BigDecimal periodAmount = depreciableBase.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

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
