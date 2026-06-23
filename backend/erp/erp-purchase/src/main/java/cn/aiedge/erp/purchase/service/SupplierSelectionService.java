package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseDemand;
import cn.aiedge.erp.purchase.entity.SupplierCandidate;
import cn.aiedge.erp.purchase.entity.SupplierEvaluation;
import cn.aiedge.erp.purchase.entity.SupplierQuotation;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 供应商筛选服务接口
 * 
 * 负责根据采购需求筛选合适的供应商，包括：
 * 1. 供应商匹配与推荐
 * 2. 供应商评分与评估
 * 3. 询价邀请管理
 * 4. 供应商绩效跟踪
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SupplierSelectionService {
    
    // ================ 供应商匹配与推荐 ================
    
    /**
     * 根据采购需求匹配供应商
     * 
     * @param demandId 采购需求ID
     * @param maxCandidates 最大候选供应商数量
     * @return 匹配的供应商候选列表
     */
    List<SupplierCandidate> matchSuppliersForDemand(Long demandId, Integer maxCandidates);
    
    /**
     * 智能推荐供应商（集成AI分析）
     * 
     * @param demandId 采购需求ID
     * @param topN 推荐数量
     * @return 推荐供应商列表及推荐理由
     */
    List<Map<String, Object>> recommendSuppliers(Long demandId, Integer topN);
    
    /**
     * 多维度供应商筛选
     * 
     * @param demandId 采购需求ID
     * @param filters 筛选条件（质量、价格、交期等）
     * @return 筛选后的供应商列表
     */
    List<SupplierCandidate> filterSuppliers(Long demandId, Map<String, Object> filters);
    
    /**
     * 供应商匹配度评分
     * 
     * @param demandId 采购需求ID
     * @param supplierId 供应商ID
     * @return 匹配度评分（0-100）
     */
    BigDecimal calculateMatchScore(Long demandId, String supplierId);
    
    // ================ 供应商评分与评估 ================
    
    /**
     * 综合评估供应商
     * 
     * @param demandId 采购需求ID
     * @param supplierId 供应商ID
     * @return 供应商评估结果
     */
    SupplierEvaluation evaluateSupplier(Long demandId, String supplierId);
    
    /**
     * 批量评估供应商
     * 
     * @param demandId 采购需求ID
     * @param supplierIds 供应商ID列表
     * @return 批量评估结果
     */
    List<SupplierEvaluation> batchEvaluateSuppliers(Long demandId, List<String> supplierIds);
    
    /**
     * 供应商风险评估
     * 
     * @param supplierId 供应商ID
     * @return 风险评估结果
     */
    Map<String, Object> assessSupplierRisk(String supplierId);
    
    /**
     * 供应商信用评估
     * 
     * @param supplierId 供应商ID
     * @return 信用评估结果
     */
    Map<String, Object> assessSupplierCredit(String supplierId);
    
    /**
     * 供应商质量评估
     * 
     * @param supplierId 供应商ID
     * @param materialCode 物料编码
     * @return 质量评估结果
     */
    Map<String, Object> assessSupplierQuality(String supplierId, String materialCode);
    
    // ================ 询价邀请管理 ================
    
    /**
     * 创建询价邀请
     * 
     * @param demandId 采购需求ID
     * @param supplierIds 供应商ID列表
     * @param inquiryDetails 询价详情
     * @return 询价邀请ID列表
     */
    List<Long> createInquiryInvitations(Long demandId, List<String> supplierIds, Map<String, Object> inquiryDetails);
    
    /**
     * 发送询价邀请
     * 
     * @param invitationIds 邀请ID列表
     * @return 发送结果
     */
    Map<String, Object> sendInquiryInvitations(List<Long> invitationIds);
    
    /**
     * 跟踪询价邀请状态
     * 
     * @param invitationId 邀请ID
     * @return 邀请状态详情
     */
    Map<String, Object> trackInvitationStatus(Long invitationId);
    
    /**
     * 更新询价邀请状态
     * 
     * @param invitationId 邀请ID
     * @param status 新状态
     * @param remarks 备注
     */
    void updateInvitationStatus(Long invitationId, String status, String remarks);
    
    // ================ 供应商报价管理 ================
    
    /**
     * 接收供应商报价
     * 
     * @param quotation 报价信息
     * @return 报价ID
     */
    Long receiveSupplierQuotation(SupplierQuotation quotation);
    
    /**
     * 比较供应商报价
     * 
     * @param demandId 采购需求ID
     * @return 报价比较结果
     */
    List<Map<String, Object>> compareQuotations(Long demandId);
    
    /**
     * 分析报价合理性
     * 
     * @param quotationId 报价ID
     * @return 合理性分析结果
     */
    Map<String, Object> analyzeQuotationRationality(Long quotationId);
    
    /**
     * 推荐最优报价
     * 
     * @param demandId 采购需求ID
     * @param evaluationCriteria 评估标准
     * @return 最优报价推荐
     */
    Map<String, Object> recommendBestQuotation(Long demandId, Map<String, BigDecimal> evaluationCriteria);
    
    // ================ 供应商绩效跟踪 ================
    
    /**
     * 记录供应商绩效
     * 
     * @param supplierId 供应商ID
     * @param performanceData 绩效数据
     * @return 绩效记录ID
     */
    Long recordSupplierPerformance(String supplierId, Map<String, Object> performanceData);
    
    /**
     * 获取供应商绩效历史
     * 
     * @param supplierId 供应商ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 绩效历史记录
     */
    List<Map<String, Object>> getSupplierPerformanceHistory(String supplierId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 计算供应商绩效评分
     * 
     * @param supplierId 供应商ID
     * @param period 统计周期（月/季度/年）
     * @return 绩效评分
     */
    Map<String, Object> calculatePerformanceScore(String supplierId, String period);
    
    /**
     * 供应商绩效排名
     * 
     * @param category 供应商类别
     * @param criteria 排名标准
     * @param limit 排名数量
     * @return 绩效排名列表
     */
    List<Map<String, Object>> rankSuppliers(String category, Map<String, Object> criteria, Integer limit);
    
    // ================ 供应商关系管理 ================
    
    /**
     * 添加供应商到候选池
     * 
     * @param supplierData 供应商数据
     * @return 供应商ID
     */
    String addSupplierToCandidatePool(Map<String, Object> supplierData);
    
    /**
     * 更新供应商信息
     * 
     * @param supplierId 供应商ID
     * @param updateData 更新数据
     */
    void updateSupplierInfo(String supplierId, Map<String, Object> updateData);
    
    /**
     * 暂停供应商资格
     * 
     * @param supplierId 供应商ID
     * @param reason 暂停原因
     * @param suspensionPeriod 暂停期限
     */
    void suspendSupplier(String supplierId, String reason, Integer suspensionPeriod);
    
    /**
     * 恢复供应商资格
     * 
     * @param supplierId 供应商ID
     * @param reason 恢复原因
     */
    void restoreSupplier(String supplierId, String reason);
    
    /**
     * 黑名单管理
     * 
     * @param supplierId 供应商ID
     * @param reason 黑名单原因
     * @param blacklistPeriod 黑名单期限
     */
    void addToBlacklist(String supplierId, String reason, Integer blacklistPeriod);
    
    /**
     * 从黑名单移除
     * 
     * @param supplierId 供应商ID
     * @param reason 移除原因
     */
    void removeFromBlacklist(String supplierId, String reason);
    
    // ================ 查询与统计 ================
    
    /**
     * 查询供应商候选列表
     * 
     * @param page 分页参数
     * @param filters 筛选条件
     * @return 供应商候选分页列表
     */
    Page<SupplierCandidate> pageSupplierCandidates(Page<SupplierCandidate> page, Map<String, Object> filters);
    
    /**
     * 查询供应商评估记录
     * 
     * @param page 分页参数
     * @param filters 筛选条件
     * @return 供应商评估分页列表
     */
    Page<SupplierEvaluation> pageSupplierEvaluations(Page<SupplierEvaluation> page, Map<String, Object> filters);
    
    /**
     * 查询供应商报价
     * 
     * @param page 分页参数
     * @param filters 筛选条件
     * @return 供应商报价分页列表
     */
    Page<SupplierQuotation> pageSupplierQuotations(Page<SupplierQuotation> page, Map<String, Object> filters);
    
    /**
     * 获取供应商统计信息
     * 
     * @param tenantId 租户ID
     * @return 供应商统计
     */
    Map<String, Object> getSupplierStatistics(Long tenantId);
    
    /**
     * 分析供应商集中度
     * 
     * @param tenantId 租户ID
     * @param materialCategory 物料类别
     * @return 集中度分析结果
     */
    Map<String, Object> analyzeSupplierConcentration(Long tenantId, String materialCategory);
    
    /**
     * 供应商地域分布分析
     * 
     * @param tenantId 租户ID
     * @return 地域分布分析
     */
    Map<String, Object> analyzeSupplierGeographicDistribution(Long tenantId);
    
    // ================ 集成与协作 ================
    
    /**
     * 与AI供应商分析模块集成
     * 
     * @param demandId 采购需求ID
     * @param supplierId 供应商ID
     * @return AI分析结果
     */
    Map<String, Object> integrateWithAIAnalysis(Long demandId, String supplierId);
    
    /**
     * 批量同步供应商数据到AI模块
     * 
     * @param supplierIds 供应商ID列表
     * @return 同步结果
     */
    Map<String, Object> syncSupplierDataToAI(List<String> supplierIds);
    
    /**
     * 获取AI预测的供应商绩效
     * 
     * @param supplierId 供应商ID
     * @param predictionPeriod 预测周期
     * @return AI预测结果
     */
    Map<String, Object> getAIPredictedPerformance(String supplierId, String predictionPeriod);
    
    /**
     * 供应商协同推荐
     * 
     * @param demandId 采购需求ID
     * @param collaborationCriteria 协同标准
     * @return 协同推荐结果
     */
    Map<String, Object> recommendCollaborativeSuppliers(Long demandId, Map<String, Object> collaborationCriteria);
    
    // ================ 批量操作 ================
    
    /**
     * 批量匹配供应商
     * 
     * @param demandIds 需求ID列表
     * @param maxCandidates 最大候选数
     * @return 批量匹配结果
     */
    Map<String, List<SupplierCandidate>> batchMatchSuppliers(List<Long> demandIds, Integer maxCandidates);
    
    /**
     * 批量评估供应商报价
     * 
     * @param quotationIds 报价ID列表
     * @return 批量评估结果
     */
    Map<String, Object> batchEvaluateQuotations(List<Long> quotationIds);
    
    /**
     * 批量更新供应商状态
     * 
     * @param supplierIds 供应商ID列表
     * @param status 新状态
     * @param reason 原因
     */
    void batchUpdateSupplierStatus(List<String> supplierIds, String status, String reason);
    
    // ================ 报告与导出 ================
    
    /**
     * 生成供应商筛选报告
     * 
     * @param demandId 采购需求ID
     * @return 筛选报告
     */
    String generateSupplierSelectionReport(Long demandId);
    
    /**
     * 生成供应商绩效报告
     * 
     * @param supplierId 供应商ID
     * @param period 报告周期
     * @return 绩效报告
     */
    String generateSupplierPerformanceReport(String supplierId, String period);
    
    /**
     * 导出供应商数据
     * 
     * @param filters 筛选条件
     * @param exportFormat 导出格式
     * @return 导出数据
     */
    List<Map<String, Object>> exportSupplierData(Map<String, Object> filters, String exportFormat);
    
    /**
     * 导出供应商评估报告
     * 
     * @param evaluationIds 评估ID列表
     * @param reportFormat 报告格式
     * @return 评估报告
     */
    String exportEvaluationReport(List<Long> evaluationIds, String reportFormat);
    
    // ================ 验证与检查 ================
    
    /**
     * 验证供应商资格
     * 
     * @param supplierId 供应商ID
     * @param qualificationCriteria 资格标准
     * @return 资格验证结果
     */
    Map<String, Boolean> validateSupplierQualification(String supplierId, Map<String, Object> qualificationCriteria);
    
    /**
     * 检查供应商合规性
     * 
     * @param supplierId 供应商ID
     * @param complianceRequirements 合规要求
     * @return 合规性检查结果
     */
    Map<String, Object> checkSupplierCompliance(String supplierId, Map<String, Object> complianceRequirements);
    
    /**
     * 验证报价完整性
     * 
     * @param quotation 报价信息
     * @return 完整性验证结果
     */
    Map<String, Boolean> validateQuotationCompleteness(SupplierQuotation quotation);
    
    /**
     * 检查供应商风险预警
     * 
     * @param supplierId 供应商ID
     * @return 风险预警检查结果
     */
    Map<String, Object> checkSupplierRiskWarning(String supplierId);
}