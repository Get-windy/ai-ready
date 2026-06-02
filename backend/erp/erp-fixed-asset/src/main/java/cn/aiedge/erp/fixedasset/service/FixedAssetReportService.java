package cn.aiedge.erp.fixedasset.service;

import java.util.List;
import java.util.Map;

/**
 * 固定资产报表服务接口
 */
public interface FixedAssetReportService {

    List<Map<String, Object>> getDepreciationSummary(String year);

    List<Map<String, Object>> getAssetLedger(String assetCode, String departmentId);

    List<Map<String, Object>> getAgeAnalysis();

    List<Map<String, Object>> getCategorySummary();
}
