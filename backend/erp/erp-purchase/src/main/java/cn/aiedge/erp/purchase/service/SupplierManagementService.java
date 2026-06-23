package cn.aiedge.erp.purchase.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 供应商管理服务接口 - 专注于采购相关的供应商业务逻辑
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SupplierManagementService {

    /**
     * 评估供应商绩效
     * 
     * @param supplierId 供应商ID
     * @param periodStart 评估开始日期
     * @param periodEnd 评估结束日期
     * @return 绩效评分和详情
     */
    Map<String, Object> evaluateSupplierPerformance(Long supplierId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 获取供应商排名
     * 
     * @param tenantId 租户ID
     * @param criteria 排名标准（交付及时率、质量合格率、价格竞争力等）
     * @param topN 前N名
     * @return 供应商排名列表
     */
    List<Map<String, Object>> getSupplierRanking(Long tenantId, String criteria, int topN);

    /**
     * 分析供应商风险
     * 
     * @param supplierId 供应商ID
     * @return 风险分析和建议
     */
    Map<String, Object> analyzeSupplierRisk(Long supplierId);

    /**
     * 计算供应商合作价值
     * 
     * @param supplierId 供应商ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 合作价值详情
     */
    Map<String, Object> calculateCooperationValue(Long supplierId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取供应商采购统计
     * 
     * @param supplierId 供应商ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 采购统计信息
     */
    Map<String, Object> getSupplierPurchaseStatistics(Long supplierId, LocalDate startDate, LocalDate endDate);

    /**
     * 生成供应商评估报告
     * 
     * @param supplierId 供应商ID
     * @param periodStart 报告期开始
     * @param periodEnd 报告期结束
     * @return 评估报告
     */
    Map<String, Object> generateSupplierEvaluationReport(Long supplierId, LocalDate periodStart, LocalDate periodEnd);

    /**
     * 根据采购需求推荐供应商
     * 
     * @param productId 产品ID
     * @param requiredQuantity 需求数量
     * @param requiredDate 需求日期
     * @param budget 预算
     * @return 推荐的供应商列表
     */
    List<Map<String, Object>> recommendSuppliers(Long productId, BigDecimal requiredQuantity, 
                                                 LocalDate requiredDate, BigDecimal budget);

    /**
     * 更新供应商绩效数据
     * 
     * @param supplierId 供应商ID
     * @param orderId 订单ID
     * @param performanceData 绩效数据（交付及时性、质量合格率等）
     */
    void updateSupplierPerformance(Long supplierId, Long orderId, Map<String, Object> performanceData);
}