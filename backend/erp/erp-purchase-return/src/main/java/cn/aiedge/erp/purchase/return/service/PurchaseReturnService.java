package cn.aiedge.erp.purchase.return.service;

import cn.aiedge.erp.purchase.return.dto.PurchaseReturnDTO;
import cn.aiedge.erp.purchase.return.entity.PurchaseReturn;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 采购换货单服务接口
 * 
 * 功能: 提供采购换货单的业务操作服务
 */
public interface PurchaseReturnService extends IService<PurchaseReturn> {
    
    /**
     * 创建采购换货单
     * 
     * @param dto 换货单数据传输对象
     * @return 创建的换货单ID
     */
    Long createPurchaseReturn(PurchaseReturnDTO dto);
    
    /**
     * 更新采购换货单
     * 
     * @param id 换货单ID
     * @param dto 更新数据
     * @return 更新是否成功
     */
    boolean updatePurchaseReturn(Long id, PurchaseReturnDTO dto);
    
    /**
     * 提交换货单审批
     * 
     * @param id 换货单ID
     * @return 审批流程实例ID
     */
    String submitForApproval(Long id);
    
    /**
     * 审批通过
     * 
     * @param id 换货单ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     * @return 审批是否成功
     */
    boolean approve(Long id, Long approverId, String approverName, String comment);
    
    /**
     * 审批拒绝
     * 
     * @param id 换货单ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 拒绝原因
     * @return 拒绝是否成功
     */
    boolean reject(Long id, Long approverId, String approverName, String comment);
    
    /**
     * 供应商确认换货方案
     * 
     * @param id 换货单ID
     * @param confirmationNote 供应商确认备注
     * @return 确认是否成功
     */
    boolean supplierConfirm(Long id, String confirmationNote);
    
    /**
     * 供应商拒绝换货方案
     * 
     * @param id 换货单ID
     * @param rejectionReason 拒绝原因
     * @return 拒绝是否成功
     */
    boolean supplierReject(Long id, String rejectionReason);
    
    /**
     * 更新退货物流信息
     * 
     * @param id 换货单ID
     * @param trackingNumber 物流单号
     * @param logisticsCompany 物流公司
     * @return 更新是否成功
     */
    boolean updateReturnLogistics(Long id, String trackingNumber, String logisticsCompany);
    
    /**
     * 更新换货物流信息
     * 
     * @param id 换货单ID
     * @param trackingNumber 物流单号
     * @param logisticsCompany 物流公司
     * @return 更新是否成功
     */
    boolean updateReplacementLogistics(Long id, String trackingNumber, String logisticsCompany);
    
    /**
     * 标记退货完成
     * 
     * @param id 换货单ID
     * @param returnQuantity 实际退货数量
     * @return 标记是否成功
     */
    boolean markReturnComplete(Long id, BigDecimal returnQuantity);
    
    /**
     * 标记换货完成
     * 
     * @param id 换货单ID
     * @param replacedQuantity 实际换货数量
     * @return 标记是否成功
     */
    boolean markReplacementComplete(Long id, BigDecimal replacedQuantity);
    
    /**
     * 完成换货单
     * 
     * @param id 换货单ID
     * @param completionNote 完成备注
     * @return 完成是否成功
     */
    boolean complete(Long id, String completionNote);
    
    /**
     * 取消换货单
     * 
     * @param id 换货单ID
     * @param cancellationReason 取消原因
     * @return 取消是否成功
     */
    boolean cancel(Long id, String cancellationReason);
    
    /**
     * 计算换货成本
     * 
     * @param id 换货单ID
     * @return 换货总成本
     */
    BigDecimal calculateReturnCost(Long id);
    
    /**
     * 更新索赔信息
     * 
     * @param id 换货单ID
     * @param claimAmount 索赔金额
     * @param claimStatus 索赔状态
     * @param claimNote 索赔备注
     * @return 更新是否成功
     */
    boolean updateClaim(Long id, BigDecimal claimAmount, String claimStatus, String claimNote);
    
    /**
     * 根据采购订单查询换货单列表
     * 
     * @param purchaseOrderCode 采购订单号
     * @return 换货单列表
     */
    List<PurchaseReturnDTO> getByPurchaseOrder(String purchaseOrderCode);
    
    /**
     * 根据供应商查询换货单列表
     * 
     * @param supplierId 供应商ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 换货单列表
     */
    List<PurchaseReturnDTO> getBySupplier(Long supplierId, String startDate, String endDate);
    
    /**
     * 查询待审批的换货单列表
     * 
     * @param userId 用户ID
     * @param role 用户角色
     * @return 待审批列表
     */
    List<PurchaseReturnDTO> getPendingApprovalList(Long userId, String role);
    
    /**
     * 查询供应商待确认的换货单列表
     * 
     * @param supplierId 供应商ID
     * @return 待确认列表
     */
    List<PurchaseReturnDTO> getPendingSupplierConfirmation(Long supplierId);
    
    /**
     * 查询超期的换货单
     * 
     * @param overdueDays 超期天数
     * @return 超期换货单列表
     */
    List<PurchaseReturnDTO> getOverdueReturns(int overdueDays);
    
    /**
     * 获取换货单统计信息
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param supplierId 供应商ID (可选)
     * @param returnType 换货类型 (可选)
     * @return 统计信息
     */
    ReturnStatisticsDTO getStatistics(String startDate, String endDate, Long supplierId, String returnType);
    
    /**
     * 导出换货单数据
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param exportType 导出类型 (excel, pdf, csv)
     * @return 导出文件路径
     */
    String exportReturns(String startDate, String endDate, String exportType);
    
    /**
     * 换货单统计DTO
     */
    class ReturnStatisticsDTO {
        private int totalCount;
        private int completedCount;
        private int pendingCount;
        private int qualityReturnCount;
        private int quantityReturnCount;
        private int specificationReturnCount;
        private BigDecimal totalAmount;
        private BigDecimal avgProcessingTime;
        private BigDecimal supplierCostShare;
        private BigDecimal buyerCostShare;
        
        // Getters and setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        
        public int getCompletedCount() { return completedCount; }
        public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }
        
        public int getPendingCount() { return pendingCount; }
        public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
        
        public int getQualityReturnCount() { return qualityReturnCount; }
        public void setQualityReturnCount(int qualityReturnCount) { this.qualityReturnCount = qualityReturnCount; }
        
        public int getQuantityReturnCount() { return quantityReturnCount; }
        public void setQuantityReturnCount(int quantityReturnCount) { this.quantityReturnCount = quantityReturnCount; }
        
        public int getSpecificationReturnCount() { return specificationReturnCount; }
        public void setSpecificationReturnCount(int specificationReturnCount) { this.specificationReturnCount = specificationReturnCount; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public BigDecimal getAvgProcessingTime() { return avgProcessingTime; }
        public void setAvgProcessingTime(BigDecimal avgProcessingTime) { this.avgProcessingTime = avgProcessingTime; }
        
        public BigDecimal getSupplierCostShare() { return supplierCostShare; }
        public void setSupplierCostShare(BigDecimal supplierCostShare) { this.supplierCostShare = supplierCostShare; }
        
        public BigDecimal getBuyerCostShare() { return buyerCostShare; }
        public void setBuyerCostShare(BigDecimal buyerCostShare) { this.buyerCostShare = buyerCostShare; }
    }
}