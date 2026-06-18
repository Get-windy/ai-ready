package cn.aiedge.erp.invoice.service;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.model.entity.PaymentRecord;
import cn.aiedge.erp.invoice.model.enums.MatchingStatus;
import cn.aiedge.erp.invoice.model.enums.MatchingType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 发票匹配服务接口
 * 提供发票与付款匹配、发票与采购订单匹配等核心功能
 */
public interface InvoiceMatchingService {
    
    /**
     * 自动匹配发票与付款记录
     * 
     * @param invoiceId 发票ID
     * @param paymentRecordId 付款记录ID
     * @param matchingType 匹配类型
     * @return 是否成功匹配
     */
    boolean autoMatchInvoiceToPayment(Long invoiceId, Long paymentRecordId, MatchingType matchingType);
    
    /**
     * 手动匹配发票与付款记录
     * 
     * @param invoiceId 发票ID
     * @param paymentRecordId 付款记录ID
     * @param matchingAmount 匹配金额
     * @param matchedBy 匹配人ID
     * @param notes 匹配说明
     * @return 是否成功匹配
     */
    boolean manualMatchInvoiceToPayment(Long invoiceId, Long paymentRecordId, BigDecimal matchingAmount, 
                                        Long matchedBy, String notes);
    
    /**
     * 匹配发票与采购订单
     * 
     * @param invoiceId 发票ID
     * @param purchaseOrderId 采购订单ID
     * @param matchedBy 匹配人ID
     * @return 是否成功匹配
     */
    boolean matchInvoiceToPurchaseOrder(Long invoiceId, Long purchaseOrderId, Long matchedBy);
    
    /**
     * 匹配发票与销售订单
     * 
     * @param invoiceId 发票ID
     * @param salesOrderId 销售订单ID
     * @param matchedBy 匹配人ID
     * @return 是否成功匹配
     */
    boolean matchInvoiceToSalesOrder(Long invoiceId, Long salesOrderId, Long matchedBy);
    
    /**
     * 部分匹配发票
     * 
     * @param invoiceId 发票ID
     * @param paymentRecordId 付款记录ID
     * @param partialAmount 部分匹配金额
     * @param matchedBy 匹配人ID
     * @param notes 匹配说明
     * @return 是否成功匹配
     */
    boolean partialMatchInvoice(Long invoiceId, Long paymentRecordId, BigDecimal partialAmount, 
                                Long matchedBy, String notes);
    
    /**
     * 取消发票匹配
     * 
     * @param invoiceId 发票ID
     * @param paymentRecordId 付款记录ID
     * @param cancelledBy 取消人ID
     * @param reason 取消原因
     * @return 是否成功取消
     */
    boolean unmatchInvoice(Long invoiceId, Long paymentRecordId, Long cancelledBy, String reason);
    
    /**
     * 获取发票的匹配状态
     * 
     * @param invoiceId 发票ID
     * @return 匹配状态
     */
    MatchingStatus getInvoiceMatchingStatus(Long invoiceId);
    
    /**
     * 获取发票的匹配详情
     * 
     * @param invoiceId 发票ID
     * @return 匹配记录列表
     */
    List<PaymentRecord> getInvoiceMatchingDetails(Long invoiceId);
    
    /**
     * 获取未匹配的发票列表
     * 
     * @param customerId 客户ID（可选）
     * @param supplierId 供应商ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 未匹配的发票列表
     */
    List<Invoice> getUnmatchedInvoices(Long customerId, Long supplierId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取部分匹配的发票列表
     * 
     * @return 部分匹配的发票列表
     */
    List<Invoice> getPartiallyMatchedInvoices();
    
    /**
     * 获取完全匹配的发票列表
     * 
     * @return 完全匹配的发票列表
     */
    List<Invoice> getFullyMatchedInvoices();
    
    /**
     * 检查发票是否可以匹配
     * 
     * @param invoice 发票对象
     * @return 是否可以匹配
     */
    boolean canMatchInvoice(Invoice invoice);
    
    /**
     * 验证匹配金额
     * 
     * @param invoiceId 发票ID
     * @param matchingAmount 匹配金额
     * @return 是否有效
     */
    boolean validateMatchingAmount(Long invoiceId, BigDecimal matchingAmount);
    
    /**
     * 获取发票的已匹配金额
     * 
     * @param invoiceId 发票ID
     * @return 已匹配金额
     */
    BigDecimal getMatchedAmount(Long invoiceId);
    
    /**
     * 获取发票的未匹配金额
     * 
     * @param invoiceId 发票ID
     * @return 未匹配金额
     */
    BigDecimal getUnmatchedAmount(Long invoiceId);
    
    /**
     * 批量匹配发票
     * 
     * @param invoiceIds 发票ID列表
     * @param paymentRecordId 付款记录ID
     * @param matchedBy 匹配人ID
     * @return 成功匹配的发票数量
     */
    int batchMatchInvoices(List<Long> invoiceIds, Long paymentRecordId, Long matchedBy);
    
    /**
     * 自动匹配到期的发票
     * 
     * @return 自动匹配的发票数量
     */
    int autoMatchDueInvoices();
    
    /**
     * 重新计算发票的匹配状态
     * 
     * @param invoiceId 发票ID
     * @return 更新后的匹配状态
     */
    MatchingStatus recalculateMatchingStatus(Long invoiceId);
    
    /**
     * 生成匹配报告
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 匹配报告内容
     */
    String generateMatchingReport(LocalDate startDate, LocalDate endDate);
    
    /**
     * 清理旧的匹配记录
     * 
     * @param olderThanDays 保留多少天内的记录
     * @return 清理的记录数量
     */
    int cleanupOldMatchingRecords(int olderThanDays);
    
    /**
     * 获取匹配统计信息
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 匹配统计
     */
    MatchingStatistics getMatchingStatistics(LocalDate startDate, LocalDate endDate);
    
    /**
     * 匹配统计信息类
     */
    class MatchingStatistics {
        private int totalInvoices;
        private int unmatchedCount;
        private int partiallyMatchedCount;
        private int fullyMatchedCount;
        private BigDecimal unmatchedAmount;
        private BigDecimal partiallyMatchedAmount;
        private BigDecimal fullyMatchedAmount;
        private int autoMatches;
        private int manualMatches;
        
        // 构造函数
        public MatchingStatistics() {
            this.totalInvoices = 0;
            this.unmatchedCount = 0;
            this.partiallyMatchedCount = 0;
            this.fullyMatchedCount = 0;
            this.unmatchedAmount = BigDecimal.ZERO;
            this.partiallyMatchedAmount = BigDecimal.ZERO;
            this.fullyMatchedAmount = BigDecimal.ZERO;
            this.autoMatches = 0;
            this.manualMatches = 0;
        }
        
        // Getter和Setter方法
        public int getTotalInvoices() { return totalInvoices; }
        public void setTotalInvoices(int totalInvoices) { this.totalInvoices = totalInvoices; }
        
        public int getUnmatchedCount() { return unmatchedCount; }
        public void setUnmatchedCount(int unmatchedCount) { this.unmatchedCount = unmatchedCount; }
        
        public int getPartiallyMatchedCount() { return partiallyMatchedCount; }
        public void setPartiallyMatchedCount(int partiallyMatchedCount) { this.partiallyMatchedCount = partiallyMatchedCount; }
        
        public int getFullyMatchedCount() { return fullyMatchedCount; }
        public void setFullyMatchedCount(int fullyMatchedCount) { this.fullyMatchedCount = fullyMatchedCount; }
        
        public BigDecimal getUnmatchedAmount() { return unmatchedAmount; }
        public void setUnmatchedAmount(BigDecimal unmatchedAmount) { this.unmatchedAmount = unmatchedAmount; }
        
        public BigDecimal getPartiallyMatchedAmount() { return partiallyMatchedAmount; }
        public void setPartiallyMatchedAmount(BigDecimal partiallyMatchedAmount) { this.partiallyMatchedAmount = partiallyMatchedAmount; }
        
        public BigDecimal getFullyMatchedAmount() { return fullyMatchedAmount; }
        public void setFullyMatchedAmount(BigDecimal fullyMatchedAmount) { this.fullyMatchedAmount = fullyMatchedAmount; }
        
        public int getAutoMatches() { return autoMatches; }
        public void setAutoMatches(int autoMatches) { this.autoMatches = autoMatches; }
        
        public int getManualMatches() { return manualMatches; }
        public void setManualMatches(int manualMatches) { this.manualMatches = manualMatches; }
        
        /**
         * 添加发票统计
         */
        public void addInvoice(Invoice invoice) {
            totalInvoices++;
            
            MatchingStatus status = invoice.getMatchingStatus();
            BigDecimal amount = invoice.getTotalAmount();
            
            switch (status) {
                case UNMATCHED:
                    unmatchedCount++;
                    unmatchedAmount = unmatchedAmount.add(amount);
                    break;
                case PARTIALLY_MATCHED:
                    partiallyMatchedCount++;
                    partiallyMatchedAmount = partiallyMatchedAmount.add(amount);
                    break;
                case FULLY_MATCHED:
                    fullyMatchedCount++;
                    fullyMatchedAmount = fullyMatchedAmount.add(amount);
                    break;
            }
        }
        
        /**
         * 添加匹配统计
         */
        public void addMatch(boolean isAuto) {
            if (isAuto) {
                autoMatches++;
            } else {
                manualMatches++;
            }
        }
        
        /**
         * 合并统计信息
         */
        public void merge(MatchingStatistics other) {
            this.totalInvoices += other.totalInvoices;
            this.unmatchedCount += other.unmatchedCount;
            this.partiallyMatchedCount += other.partiallyMatchedCount;
            this.fullyMatchedCount += other.fullyMatchedCount;
            this.unmatchedAmount = this.unmatchedAmount.add(other.unmatchedAmount);
            this.partiallyMatchedAmount = this.partiallyMatchedAmount.add(other.partiallyMatchedAmount);
            this.fullyMatchedAmount = this.fullyMatchedAmount.add(other.fullyMatchedAmount);
            this.autoMatches += other.autoMatches;
            this.manualMatches += other.manualMatches;
        }
        
        @Override
        public String toString() {
            return String.format(
                "MatchingStatistics{totalInvoices=%d, unmatched=%d(%.2f), partially=%d(%.2f), fully=%d(%.2f), auto=%d, manual=%d}",
                totalInvoices, unmatchedCount, unmatchedAmount, partiallyMatchedCount, partiallyMatchedAmount,
                fullyMatchedCount, fullyMatchedAmount, autoMatches, manualMatches
            );
        }
    }
}