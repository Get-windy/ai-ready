package cn.aiedge.erp.invoice.model.entity;

import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.model.enums.InvoiceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 发票申请实体类
 * 表示发票申请记录，包括销售发票和采购发票申请
 */
@Entity
@Table(name = "invoice_application")
@Data
@EqualsAndHashCode(callSuper = false)
public class InvoiceApplication extends BaseEntity {
    
    /**
     * 申请单号 - 系统自动生成
     */
    @Column(name = "application_number", nullable = false, unique = true, length = 50)
    private String applicationNumber;
    
    /**
     * 关联订单ID
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    /**
     * 订单类型 - SALES_ORDER / PURCHASE_ORDER
     */
    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;
    
    /**
     * 客户ID - 对于销售发票
     */
    @Column(name = "customer_id")
    private Long customerId;
    
    /**
     * 客户名称
     */
    @Column(name = "customer_name", length = 200)
    private String customerName;
    
    /**
     * 客户税号
     */
    @Column(name = "customer_tax_number", length = 50)
    private String customerTaxNumber;
    
    /**
     * 客户地址
     */
    @Column(name = "customer_address", length = 500)
    private String customerAddress;
    
    /**
     * 客户电话
     */
    @Column(name = "customer_phone", length = 50)
    private String customerPhone;
    
    /**
     * 客户银行账户
     */
    @Column(name = "customer_bank_account", length = 100)
    private String customerBankAccount;
    
    /**
     * 供应商ID - 对于采购发票
     */
    @Column(name = "supplier_id")
    private Long supplierId;
    
    /**
     * 供应商名称
     */
    @Column(name = "supplier_name", length = 200)
    private String supplierName;
    
    /**
     * 供应商税号
     */
    @Column(name = "supplier_tax_number", length = 50)
    private String supplierTaxNumber;
    
    /**
     * 供应商地址
     */
    @Column(name = "supplier_address", length = 500)
    private String supplierAddress;
    
    /**
     * 供应商电话
     */
    @Column(name = "supplier_phone", length = 50)
    private String supplierPhone;
    
    /**
     * 供应商银行账户
     */
    @Column(name = "supplier_bank_account", length = 100)
    private String supplierBankAccount;
    
    /**
     * 发票类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 20)
    private InvoiceType invoiceType;
    
    /**
     * 发票状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;
    
    /**
     * 币种代码 - 默认CNY
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "CNY";
    
    /**
     * 汇率 - 相对于本位币
     */
    @Column(name = "exchange_rate", precision = 12, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;
    
    /**
     * 不含税金额
     */
    @Column(name = "subtotal_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;
    
    /**
     * 税额
     */
    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    /**
     * 折扣金额
     */
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    /**
     * 运费
     */
    @Column(name = "shipping_amount", precision = 15, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    
    /**
     * 其他费用
     */
    @Column(name = "other_amount", precision = 15, scale = 2)
    private BigDecimal otherAmount = BigDecimal.ZERO;
    
    /**
     * 总金额（含税）
     */
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    /**
     * 申请人ID
     */
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;
    
    /**
     * 申请人姓名
     */
    @Column(name = "applicant_name", length = 100)
    private String applicantName;
    
    /**
     * 申请部门
     */
    @Column(name = "applicant_department", length = 100)
    private String applicantDepartment;
    
    /**
     * 申请日期
     */
    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;
    
    /**
     * 期望开票日期
     */
    @Column(name = "expected_invoice_date")
    private LocalDate expectedInvoiceDate;
    
    /**
     * 期望付款日期
     */
    @Column(name = "expected_payment_date")
    private LocalDate expectedPaymentDate;
    
    /**
     * 付款条件
     */
    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;
    
    /**
     * 交货条件
     */
    @Column(name = "delivery_terms", length = 200)
    private String deliveryTerms;
    
    /**
     * 运输方式
     */
    @Column(name = "shipping_method", length = 100)
    private String shippingMethod;
    
    /**
     * 运输地址
     */
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;
    
    /**
     * 发票地址
     */
    @Column(name = "billing_address", length = 500)
    private String billingAddress;
    
    /**
     * 联系人
     */
    @Column(name = "contact_person", length = 100)
    private String contactPerson;
    
    /**
     * 联系电话
     */
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;
    
    /**
     * 联系邮箱
     */
    @Column(name = "contact_email", length = 100)
    private String contactEmail;
    
    /**
     * 备注
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    /**
     * 附件路径
     */
    @Column(name = "attachment_paths", columnDefinition = "TEXT")
    private String attachmentPaths;
    
    /**
     * 提交时间
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    /**
     * 提交人ID
     */
    @Column(name = "submitted_by")
    private Long submittedBy;
    
    /**
     * 审批时间
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    /**
     * 审批人ID
     */
    @Column(name = "approved_by")
    private Long approvedBy;
    
    /**
     * 审批意见
     */
    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;
    
    /**
     * 拒绝时间
     */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    
    /**
     * 拒绝人ID
     */
    @Column(name = "rejected_by")
    private Long rejectedBy;
    
    /**
     * 拒绝原因
     */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    /**
     * 业务区域
     */
    @Column(name = "business_region", length = 50)
    private String businessRegion;
    
    /**
     * 业务部门
     */
    @Column(name = "business_department", length = 100)
    private String businessDepartment;
    
    /**
     * 项目编号
     */
    @Column(name = "project_code", length = 50)
    private String projectCode;
    
    /**
     * 合同编号
     */
    @Column(name = "contract_number", length = 50)
    private String contractNumber;
    
    /**
     * 是否紧急
     */
    @Column(name = "is_urgent")
    private Boolean isUrgent = false;
    
    /**
     * 优先级 - 1-5，1为最高
     */
    @Column(name = "priority_level")
    private Integer priorityLevel = 3;
    
    /**
     * 是否内部开票
     */
    @Column(name = "is_internal")
    private Boolean isInternal = false;
    
    /**
     * 是否已生成发票
     */
    @Column(name = "invoice_generated")
    private Boolean invoiceGenerated = false;
    
    /**
     * 生成的发票ID
     */
    @Column(name = "invoice_id")
    private Long invoiceId;
    
    /**
     * 发票申请行项
     */
    @OneToMany(mappedBy = "invoiceApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceApplicationItem> items = new ArrayList<>();
    
    /**
     * 发票申请工作流记录
     */
    @OneToMany(mappedBy = "invoiceApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceWorkflow> workflows = new ArrayList<>();
    
    /**
     * 计算净额（不含税）
     */
    public BigDecimal calculateNetAmount() {
        return subtotalAmount
            .subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO)
            .add(shippingAmount != null ? shippingAmount : BigDecimal.ZERO)
            .add(otherAmount != null ? otherAmount : BigDecimal.ZERO);
    }
    
    /**
     * 计算总额（含税）
     */
    public BigDecimal calculateTotalAmount() {
        BigDecimal netAmount = calculateNetAmount();
        return netAmount.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
    }
    
    /**
     * 验证申请单数据
     */
    public boolean validate() {
        if (applicationNumber == null || applicationNumber.trim().isEmpty()) {
            return false;
        }
        
        if (orderId == null) {
            return false;
        }
        
        if (orderType == null || orderType.trim().isEmpty()) {
            return false;
        }
        
        if (invoiceType == null) {
            return false;
        }
        
        if (applicantId == null) {
            return false;
        }
        
        if (applicationDate == null) {
            return false;
        }
        
        // 验证金额
        if (subtotalAmount == null || subtotalAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (taxAmount == null || taxAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }
        
        // 验证销售发票必须有客户
        if (invoiceType.isSalesType() && customerId == null) {
            return false;
        }
        
        // 验证采购发票必须有供应商
        if (invoiceType.isPurchaseType() && supplierId == null) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查是否可以提交
     */
    public boolean canSubmit() {
        return status == InvoiceStatus.DRAFT && validate();
    }
    
    /**
     * 检查是否可以审批
     */
    public boolean canApprove() {
        return status == InvoiceStatus.SUBMITTED || status == InvoiceStatus.IN_APPROVAL;
    }
    
    /**
     * 检查是否可以拒绝
     */
    public boolean canReject() {
        return status == InvoiceStatus.SUBMITTED || status == InvoiceStatus.IN_APPROVAL;
    }
    
    /**
     * 检查是否可以取消
     */
    public boolean canCancel() {
        return status == InvoiceStatus.DRAFT || 
               status == InvoiceStatus.SUBMITTED || 
               status == InvoiceStatus.IN_APPROVAL;
    }
    
    /**
     * 检查是否可以生成发票
     */
    public boolean canGenerateInvoice() {
        return status == InvoiceStatus.APPROVED && !invoiceGenerated;
    }
    
    /**
     * 获取客户或供应商显示名称
     */
    public String getPartyDisplayName() {
        if (customerId != null) {
            return "客户[" + customerId + "]";
        } else if (supplierId != null) {
            return "供应商[" + supplierId + "]";
        }
        return "未指定";
    }
    
    /**
     * 获取金额显示字符串
     */
    public String getAmountDisplay() {
        return String.format("%s %.2f", currencyCode, totalAmount);
    }
    
    /**
     * 获取状态显示名称
     */
    public String getStatusDisplay() {
        return status.getChineseName();
    }
    
    /**
     * 获取发票类型显示名称
     */
    public String getInvoiceTypeDisplay() {
        return invoiceType.getChineseName();
    }
    
    @Override
    public String getEntityType() {
        return "INVOICE_APPLICATION";
    }
    
    @Override
    public String getDisplayName() {
        return String.format("Invoice Application[%s]", applicationNumber);
    }
}