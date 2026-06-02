package cn.aiedge.erp.fixedasset.service.depreciation;

import cn.aiedge.erp.fixedasset.model.FixedAsset;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 折旧策略接口
 */
public interface DepreciationStrategy {

    /**
     * 计算折旧
     *
     * @param asset 固定资产
     * @return 包含 periodAmount, accumulatedDepreciation, netValue 的Map
     */
    Map<String, BigDecimal> calculateDepreciation(FixedAsset asset);
}
