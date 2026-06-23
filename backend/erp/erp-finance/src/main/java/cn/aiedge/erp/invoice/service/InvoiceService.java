package cn.aiedge.erp.invoice.service;

import cn.aiedge.erp.invoice.model.entity.Invoice;
import cn.aiedge.erp.invoice.model.entity.InvoiceApplication;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 发票服务接口
 * 提供发票相关的业务逻辑操作
 */
public interface InvoiceService {
    
    /**
     * 根据ID获取发票
     * 
     * @param id 发票ID
     * @return 发票对象（如果存在）
     */
    Optional<Invoice> getInvoiceById(Long id);
    
    /**
     * 根据发票号码获取发票
     * 
     * @param invoiceNumber 发票号码
     * @return 发票对象（如果存在）
     */
    Optional<Invoice> getInvoiceByNumber(String invoiceNumber);
    
    /**
     * 获取所有发票
     * 
     * @return 发票列表
     */
    List<Invoice> getAllInvoices();
    
    /**
     * 分页获取发票
     * 
     * @param pageable 分页参数
     * @return 发票分页
     */
    Page<Invoice> getInvoices(Pageable pageable);
    
    /**
     * 根据状态获取发票
     * 
     * @param status 发票状态
     * @return 发票列表
     */
    List<Invoice> getInvoicesByStatus(InvoiceStatus status);
    
    /**
     * 根据付款状态获取发票
     * 
     * @param paymentStatus 付款状态
     * @return 发票列表
     */
    List<Invoice> getInvoicesByPaymentStatus(PaymentStatus paymentStatus);
    
    /**
     * 根据客户ID获取发票
     * 
     * @param customerId 客户ID
     * @return 发票列表
     */
    List<Invoice> getInvoicesByCustomerId(Long customerId);
    
    /**
     * 根据供应商ID获取发票
     * 
     * @param supplierId 供应商ID
     * @return 发票列表
     */
    List<Invoice> getInvoicesBySupplierId(Long supplierId);
    
    /**
     * 根据日期范围获取发票
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 发票列表
     */
    List<Invoice> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate);
    
    /**
     * 根据金额范围获取发票
     * 
     * @param minAmount 最小金额
     * @param maxAmount 最大金额
     * @return 发票列表
     */
    List<Invoice> getInvoicesByAmountRange(BigDecimal minAmount, BigDecimal maxAmount);
    
    /**
     * 创建发票（从发票申请）
     * 
     * @param application 发票申请
     * @param issuedBy 开票人ID
     * @param issuedByName 开票人姓名
     * @return 创建的发票
     */
    Invoice createInvoiceFromApplication(InvoiceApplication application, Long issuedBy, String issuedByName);
    
    /**
     * 更新发票状态
     * 
     * @param invoiceId 发票ID
     * @param newStatus 新状态
     * @param notes 状态变更说明
     * @return 是否成功更新
     */
    boolean updateInvoiceStatus(Long invoiceId, InvoiceStatus newStatus, String notes);
    
    /**
     * 更新付款状态
     * 
     * @param invoiceId 发票ID
     * @param newStatus 新付款状态
     * @param notes 状态变更说明
     * @return 是否成功更新
     */
    boolean updatePaymentStatus(Long invoiceId, PaymentStatus newStatus, String notes);
    
    /**
     * 记录付款
     * 
     * @param invoiceId 发票ID
     * @param amount 付款金额
     * @param paymentReference 付款参考号
     * @param notes 付款说明
     * @return 是否成功记录
     */
    boolean recordPayment(Long invoiceId, BigDecimal amount, String paymentReference, String notes);
    
    /**
     * 记录部分退款
     * 
     * @param invoiceId 发票ID
     * @param amount 退款金额
     * @param refundReference 退款参考号
     * @param reason 退款原因
     * @return 是否成功记录
     */
    boolean recordPartialRefund(Long invoiceId, BigDecimal amount, String refundReference, String reason);
    
    /**
     * 记录全额退款
     * 
     * @param invoiceId 发票ID
     * @param refundReference 退款参考号
     * @param reason 退款原因
     * @return 是否成功记录
     */
    boolean recordFullRefund(Long invoiceId, String refundReference, String reason);
    
    /**
     * 作废发票
     * 
     * @param invoiceId 发票ID
     * @param reason 作废原因
     * @param voidedBy 作废人ID
     * @return 是否成功作废
     */
    boolean voidInvoice(Long invoiceId, String reason, Long voidedBy);
    
    /**
     * 冲红发票
     * 
     * @param invoiceId 发票ID
     * @param reason 冲红原因
     * @param creditedBy 冲红人ID
     * @return 冲红后的新发票
     */
    Invoice creditInvoice(Long invoiceId, String reason, Long creditedBy);
    
    /**
     * 发送发票
     * 
     * @param invoiceId 发票ID
     * @param sendMethod 发送方式（EMAIL/SMS/POSTAL/PORTAL）
     * @param sentBy 发送人ID
     * @return 是否成功发送
     */
    boolean sendInvoice(Long invoiceId, String sendMethod, Long sentBy);
    
    /**
     * 重新发送发票
     * 
     * @param invoiceId 发票ID
     * @param sendMethod 发送方式
     * @param sentBy 发送人ID
     * @return 是否成功重新发送
     */
    boolean resendInvoice(Long invoiceId, String sendMethod, Long sentBy);
    
    /**
     * 下载发票文档
     * 
     * @param invoiceId 发票ID
     * @return 文档字节数组
     */
    byte[] downloadInvoiceDocument(Long invoiceId);
    
    /**
     * 生成新的发票号码
     * 
     * @return 发票号码
     */
    String generateInvoiceNumber();
    
    /**
     * 验证发票数据
     * 
     * @param invoice 发票对象
     * @return 是否有效
     */
    boolean validateInvoice(Invoice invoice);
    
    /**
     * 检查发票是否逾期
     * 
     * @param invoice 发票对象
     * @return 是否逾期
     */
    boolean checkInvoiceOverdue(Invoice invoice);
    
    /**
     * 计算逾期罚金
     * 
     * @param invoice 发票对象
     * @return 逾期罚金金额
     */
    BigDecimal calculateLateFee(Invoice invoice);
    
    /**
     * 计算逾期罚金（指定罚息率）
     * 
     * @param invoice 发票对象
     * @param dailyRate 日罚息率
     * @return 逾期罚金金额
     */
    BigDecimal calculateLateFee(Invoice invoice, BigDecimal dailyRate);
    
    /**
     * 更新发票的逾期状态
     * 
     * @param invoiceId 发票ID
     * @return 是否逾期
     */
    boolean updateOverdueStatus(Long invoiceId);
    
    /**
     * 批量更新逾期状态
     * 
     * @return 更新的发票数量
     */
    int batchUpdateOverdueStatus();
    
    /**
     * 获取发票摘要信息
     * 
     * @param invoiceId 发票ID
     * @return 摘要信息
     */
    String getInvoiceSummary(Long invoiceId);
    
    /**
     * 获取发票统计信息
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 统计信息
     */
    InvoiceStatistics getInvoiceStatistics(LocalDate startDate, LocalDate endDate);
    
    /**
     * 搜索发票
     * 
     * @param keyword 关键词
     * @return 发票列表
     */
    List<Invoice> searchInvoices(String keyword);
    
    /**
     * 导出发票数据
     * 
     * @param invoiceIds 发票ID列表
     * @param format 导出格式（PDF/EXCEL/CSV/XML）
     * @return 导出数据字节数组
     */
    byte[] exportInvoices(List<Long> invoiceIds, String format);
    
    /**
     * 批量生成发票
     * 
     * @param applicationIds 申请ID列表
     * @param issuedBy 开票人ID
     * @param issuedByName 开票人姓名
     * @return 生成的发票列表
     */
    List<Invoice> batchGenerateInvoices(List<Long> applicationIds, Long issuedBy, String issuedByName);
    
    /**
     * 删除发票（逻辑删除）
     * 
     * @param invoiceId 发票ID
     * @param deletedBy 删除人ID
     * @return 是否成功删除
     */
    boolean deleteInvoice(Long invoiceId, String deletedBy);
    
    /**
     * 恢复已删除的发票
     * 
     * @param invoiceId 发票ID
     * @return 是否成功恢复
     */
    boolean restoreInvoice(Long invoiceId);
    
    /**
     * 永久删除发票（物理删除）
     * 
     * @param invoiceId 发票ID
     * @return 是否成功删除
     */
    boolean permanentlyDeleteInvoice(Long invoiceId);
    
    /**
     * 发票统计信息类
     */
    class InvoiceStatistics {
        private long totalCount;
        private BigDecimal totalAmount;
        private BigDecimal totalTax;
        private BigDecimal totalPaid;
        private BigDecimal totalUnpaid;
        private long overdueCount;
        private BigDecimal overdueAmount;
        private long pendingCount;
        private BigDecimal pendingAmount;
        
        // 构造函数
        public InvoiceStatistics() {
            this.totalCount = 0;
            this.totalAmount = BigDecimal.ZERO;
            this.totalTax = BigDecimal.ZERO;
            this.totalPaid = BigDecimal.ZERO;
            this.totalUnpaid = BigDecimal.ZERO;
            this.overdueCount = 0;
            this.overdueAmount = BigDecimal.ZERO;
            this.pendingCount = 0;
            this.pendingAmount = BigDecimal.ZERO;
        }
        
        // Getter和Setter方法
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        
        public BigDecimal getTotalTax() { return totalTax; }
        public void setTotalTax(BigDecimal totalTax) { this.totalTax = totalTax; }
        
        public BigDecimal getTotalPaid() { return totalPaid; }
        public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
        
        public BigDecimal getTotalUnpaid() { return totalUnpaid; }
        public void setTotalUnpaid(BigDecimal totalUnpaid) { this.totalUnpaid = totalUnpaid; }
        
        public long getOverdueCount() { return overdueCount; }
        public void setOverdueCount(long overdueCount) { this.overdueCount = overdueCount; }
        
        public BigDecimal getOverdueAmount() { return overdueAmount; }
        public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }
        
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
        
        public BigDecimal getPendingAmount() { return pendingAmount; }
        public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
        
        /**
         * 添加发票统计数据
         */
        public void addInvoice(Invoice invoice) {
            totalCount++;
            totalAmount = totalAmount.add(invoice.getTotalAmount());
            totalTax = totalTax.add(invoice.getTaxAmount());
            totalPaid = totalPaid.add(invoice.getPaidAmount());
            totalUnpaid = totalUnpaid.add(invoice.getUnpaidAmount());
            
            if (invoice.getPaymentStatus() == PaymentStatus.OVERDUE) {
                overdueCount++;
                overdueAmount = overdueAmount.add(invoice.getUnpaidAmount());
            }
            
            if (invoice.getPaymentStatus() == PaymentStatus.PENDING || 
                invoice.getPaymentStatus() == PaymentStatus.PARTIALLY_PAID) {
                pendingCount++;
                pendingAmount = pendingAmount.add(invoice.getUnpaidAmount());
            }
        }
        
        /**
         * 合并统计信息
         */
        public void merge(InvoiceStatistics other) {
            this.totalCount += other.totalCount;
            this.totalAmount = this.totalAmount.add(other.totalAmount);
            this.totalTax = this.totalTax.add(other.totalTax);
            this.totalPaid = this.totalPaid.add(other.totalPaid);
            this.totalUnpaid = this.totalUnpaid.add(other.totalUnpaid);
            this.overdueCount += other.overdueCount;
            this.overdueAmount = this.overdueAmount.add(other.overdueAmount);
            this.pendingCount += other.pendingCount;
            this.pendingAmount = this.pendingAmount.add(other.pendingAmount);
        }
        
        @Override
        public String toString() {
            return String.format(
                "InvoiceStatistics{totalCount=%d, totalAmount=%.2f, totalTax=%.2f, totalPaid=%.2f, totalUnpaid=%.2f, overdueCount=%d, overdueAmount=%.2f, pendingCount=%d, pendingAmount=%.2f}",
                totalCount, totalAmount, totalTax, totalPaid, totalUnpaid, overdueCount, overdueAmount, pendingCount, pendingAmount
            );
        }
    }
}