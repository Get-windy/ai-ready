package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.entity.PurchaseDemand;
import cn.aiedge.erp.purchase.enums.DemandPriority;
import cn.aiedge.erp.purchase.enums.DemandStatus;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 采购需求分析服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PurchaseDemandAnalysisService extends IService<PurchaseDemand> {
    
    /**
     * 创建采购需求
     * 
     * @param demand 需求信息
     * @return 需求ID
     */
    Long createDemand(PurchaseDemand demand);
    
    /**
     * 更新采购需求
     * 
     * @param demand 需求信息
     */
    void updateDemand(PurchaseDemand demand);
    
    /**
     * 删除采购需求
     * 
     * @param demandId 需求ID
     */
    void deleteDemand(Long demandId);
    
    /**
     * 提交需求审批
     * 
     * @param demandId 需求ID
     */
    void submitForApproval(Long demandId);
    
    /**
     * 审批通过需求
     * 
     * @param demandId 需求ID
     */
    void approveDemand(Long demandId);
    
    /**
     * 审批拒绝需求
     * 
     * @param demandId 需求ID
     * @param reason 拒绝原因
     */
    void rejectDemand(Long demandId, String reason);
    
    /**
     * 将需求转为询价
     * 
     * @param demandId 需求ID
     * @return 询价单ID
     */
    Long convertToInquiry(Long demandId);
    
    /**
     * 批量将需求转为询价
     * 
     * @param demandIds 需求ID列表
     * @return 询价单ID
     */
    Long batchConvertToInquiry(List<Long> demandIds);
    
    /**
     * 取消需求
     * 
     * @param demandId 需求ID
     * @param reason 取消原因
     */
    void cancelDemand(Long demandId, String reason);
    
    /**
     * 分页查询采购需求
     * 
     * @param page 分页参数
     * @param tenantId 租户ID
     * @param demandNo 需求编号
     * @param materialCode 物料编码
     * @param status 状态
     * @param priority 优先级
     * @return 分页结果
     */
    Page<PurchaseDemand> pageDemands(Page<PurchaseDemand> page, Long tenantId,
                                     String demandNo, String materialCode, 
                                     DemandStatus status, DemandPriority priority);
    
    /**
     * 获取需求详情
     * 
     * @param demandId 需求ID
     * @return 需求详情
     */
    PurchaseDemand getDemandDetail(Long demandId);
    
    /**
     * 分析需求紧迫性
     * 
     * @param demandId 需求ID
     * @return 紧迫性分析结果
     */
    Map<String, Object> analyzeDemandUrgency(Long demandId);
    
    /**
     * 预测需求满足时间
     * 
     * @param demandId 需求ID
     * @return 预测结果
     */
    Map<String, Object> predictFulfillmentTime(Long demandId);
    
    /**
     * 分析需求合理性
     * 
     * @param demandId 需求ID
     * @return 合理性分析结果
     */
    Map<String, Object> analyzeDemandRationality(Long demandId);
    
    /**
     * 自动合并相似需求
     * 
     * @param tenantId 租户ID
     * @return 合并结果
     */
    Map<String, Object> autoMergeSimilarDemands(Long tenantId);
    
    /**
     * 智能推荐采购策略
     * 
     * @param demandId 需求ID
     * @return 策略推荐结果
     */
    Map<String, Object> recommendPurchaseStrategy(Long demandId);
    
    /**
     * 分析采购需求趋势
     * 
     * @param tenantId 租户ID
     * @param materialId 物料ID
     * @param days 天数
     * @return 趋势分析结果
     */
    Map<String, Object> analyzeDemandTrend(Long tenantId, Long materialId, Integer days);
    
    /**
     * 统计采购需求数据
     * 
     * @param tenantId 租户ID
     * @return 统计结果
     */
    Map<String, Object> getDemandStatistics(Long tenantId);
    
    /**
     * 生成采购需求分析报告
     * 
     * @param tenantId 租户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分析报告
     */
    String generateDemandAnalysisReport(Long tenantId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 计算采购需求预算
     * 
     * @param demandId 需求ID
     * @return 预算计算结果
     */
    Map<String, BigDecimal> calculateDemandBudget(Long demandId);
    
    /**
     * 验证需求数据完整性
     * 
     * @param demand 需求数据
     * @return 验证结果
     */
    Map<String, Boolean> validateDemandData(PurchaseDemand demand);
    
    /**
     * 批量导入采购需求
     * 
     * @param demands 需求列表
     * @return 导入结果
     */
    Map<String, Object> batchImportDemands(List<PurchaseDemand> demands);
    
    /**
     * 导出采购需求
     * 
     * @param tenantId 租户ID
     * @param status 状态
     * @return 导出数据
     */
    List<Map<String, Object>> exportDemands(Long tenantId, DemandStatus status);
    
    /**
     * 检查需求是否已满足
     * 
     * @param demandId 需求ID
     * @return 是否已满足
     */
    boolean isDemandFulfilled(Long demandId);
    
    /**
     * 更新需求关联信息
     * 
     * @param demandId 需求ID
     * @param inquiryId 询价单ID
     * @param orderId 订单ID
     */
    void updateDemandRelations(Long demandId, Long inquiryId, Long orderId);
    
    /**
     * 搜索采购需求
     * 
     * @param keyword 关键词
     * @param tenantId 租户ID
     * @return 搜索结果
     */
    List<PurchaseDemand> searchDemands(String keyword, Long tenantId);
}