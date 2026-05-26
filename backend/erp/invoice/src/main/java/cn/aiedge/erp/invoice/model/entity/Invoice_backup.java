package cn.aiedge.erp.invoice.model.entity;

import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.model.enums.InvoiceType;
import cn.aiedge.erp.invoice.model.enums.PaymentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 发票实体类
 * 表示已生成的发票记录
 */
@Entity
@Table(name = "invoice")
@Data
@EqualsAndHashCode(callSuper = false)
public class Invoice extends BaseEntity {
    
    /**
     * 发票号码 - 系统自动生成
     */
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;
    
    /**
     * 关联的发票申请ID
     */
    @Column(name = "application_id", nullable = false)
    private Long applicationId;
    
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
    @Column(name = "invoice_status", nullable = false, length = 20)
    private InvoiceStatus invoiceStatus = InvoiceStatus.GENERATED;
    
    /**
     * 付款状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;
    
    /**
     * 发票日期
     */
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;
    
    /**
     * 到期日期
     */
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
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
     * 币种代码
     */
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "CNY";
    
    /**
     * 汇率
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
     * 已付金额
     */
    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    
    /**
     * 未付金额
     */
    @Column(name = "unpaid_amount", precision = 15, scale = 2)
    private BigDecimal unpaidAmount = BigDecimal.ZERO;
    
    /**
     * 逾期天数
     */
    @Column(name = "overdue_days")
    private Integer overdueDays = 0;
    
    /**
     * 逾期罚金
     */
    @Column(name = "late_fee_amount", precision = 15, scale = 2)
    private BigDecimal lateFeeAmount = BigDecimal.ZERO;
    
    /**
     * 开票人ID
     */
    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;
    
    /**
     * 开票人姓名
     */
    @Column(name = "issued_by_name", length = 100)
    private String issuedByName;
    
    /**
     * 开票时间
     */
    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
    
    /**
     * 复核人ID
     */
    @Column(name = "reviewed_by")
    private Long reviewedBy;
    
    /**
     * 复核人姓名
     */
    @Column(name = "reviewed_by_name", length = 100)
    private String reviewedByName;
    
    /**
     * 复核时间
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
    
    /**
     * 复核意见
     */
    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
    
    /**
     * 发送时间
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    /**
     * 发送方式 - EMAIL / SMS / POSTAL / PORTAL
     */
    @Column(name = "send_method", length = 20)
    private String sendMethod;
    
    /**
     * 发送人ID
     */
    @Column(name = "sent_by")
    private Long sentBy;
    
    /**
     * 发送状态 - SENT / FAILED / PENDING
     */
    @Column(name = "send_status", length = 20)
    private String sendStatus;
    
    /**
     * 发送错误信息
     */
    @Column(name = "send_error_message", columnDefinition = "TEXT")
    private String sendErrorMessage;
    
    /**
     * 文档存储路径
     */
    @Column(name = "document_path", length = 500)
    private String documentPath;
    
    /**
     * 文档格式 - PDF / EXCEL / XML
     */
    @Column(name = "document_format", length = 20)
    private String documentFormat;
    
    /**
     * 文档大小（字节）
     */
    @Column(name = "document_size")
    private Long documentSize;
    
    /**
     * QR码数据
     */
    @Column(name = "qrcode_data", columnDefinition = "TEXT")
    private String qrcodeData;
    
    /**
     * QR码图片路径
     */
    @Column(name = "qrcode_image_path", length = 500)
    private String qrcodeImagePath;
    
    /**
     * 数字签名
     */
    @Column(name = "digital_signature", columnDefinition = "TEXT")
    private String digitalSignature;
    
    /**
     * 签名时间
     */
    @Column(name = "signed_at")
    private LocalDateTime signedAt;
    
    /**
     * 签名人ID
     */
    @Column(name = "signed_by")
    private Long signedBy;
    
    /**
     * 税务登记地区
     */
    @Column(name = "tax_region", length = 100)
    private String taxRegion;
    
    /**
     * 税率类型
     */
    @Column(name = "tax_type", length = 50)
    private String taxType;
    
    /**
     * 是否含税
     */
    @Column(name = "is_tax_inclusive")
    private Boolean isTaxInclusive = true;
    
    /**
     * 是否红冲发票
     */
    @Column(name = "is_credit_note")
    private Boolean isCreditNote = false;
    
    /**
     * 红冲原因
     */
    @Column(name = "credit_reason", length = 200)
    private String creditReason;
    
    /**
     * 红冲的原发票ID
     */
    @Column(name = "original_invoice_id")
    private Long originalInvoiceId;
    
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
     * 订单编号
     */
    @Column(name = "order_number", length = 50)
    private String orderNumber;
    
    /**
     * 发货单号
     */
    @Column(name = "delivery_number", length = 50)
    private String deliveryNumber;
    
    /**
     * 付款条件
     */
    @Column(name = "payment_terms", length = 200)
    private String paymentTerms;
    
    /**
     * 发票行项
     */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceItem> items = new ArrayList<>();
    
    /**
     * 税项明细
     */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoiceTax> taxes = new ArrayList<>();
    
    /**
     * 付款记录
     */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvoicePayment> payments = new ArrayList<>();
    
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
     * 更新未付金额
     */
    public void updateUnpaidAmount() {
        if (totalAmount != null && paidAmount != null) {
            unpaidAmount = totalAmount.subtract(paidAmount);
            
            // 更新付款状态
            if (unpaidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                paymentStatus = PaymentStatus.PAID;
            } else if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentStatus = PaymentStatus.PARTIALLY_PAID;
            } else {
                paymentStatus = PaymentStatus.PENDING;
            }
            
            // 检查是否逾期
            if (dueDate != null && LocalDate.now().isAfter(dueDate) && 
                unpaidAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentStatus = PaymentStatus.OVERDUE;
                overdueDays = (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, LocalDate.now());
            }
        }
    }
    
    /**
     * 记录付款
     */
    public void recordPayment(BigDecimal paymentAmount) {
        if (paidAmount == null) {
            paidAmount = BigDecimal.ZERO;
        }
        paidAmount = paidAmount.add(paymentAmount);
        updateUnpaidAmount();
    }
    
    /**
     * 验证发票数据
     */
    @Override
    public boolean validate() {
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            return false;
        }
        
        if (applicationId == null) {
            return false;
        }
        
        if (invoiceType == null) {
            return false;
        }
        
        if (invoiceDate == null) {
            return false;
        }
        
        if (dueDate == null) {
            return false;
        }
        
        if (issuedBy == null) {
            return false;
        }
        
        if (issuedAt == null) {
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
        
        // 验证销售发票必须有客户信息
        if (invoiceType.isSalesType() && (customerId == null || customerName == null)) {
            return false;
        }
        
        // 验证采购发票必须有供应商信息
        if (invoiceType.isPurchaseType() && (supplierId == null || supplierName == null)) {
            return false;
        }
        
        // 验证税务发票必须有税号
        if (invoiceType.requiresTaxRegistrationNumber()) {
            if (invoiceType.isSalesType() && (customerTaxNumber == null || customerTaxNumber.trim().isEmpty())) {
                return false;
            }
            if (invoiceType.isPurchaseType() && (supplierTaxNumber == null || supplierTaxNumber.trim().isEmpty())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查是否可以发送
     */
    public boolean canSend() {
        return invoiceStatus == InvoiceStatus.GENERATED && 
               paymentStatus != PaymentStatus.PAID &&
               !Boolean.TRUE.equals(isCreditNote);
    }
    
    /**
     * 检查是否可以支付
     */
    public boolean canPay() {
        return (invoiceStatus == InvoiceStatus.GENERATED || invoiceStatus == InvoiceStatus.SENT) && 
               (paymentStatus == PaymentStatus.PENDING || paymentStatus == PaymentStatus.PARTIALLY_PAID) &&
               unpaidAmount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * 检查是否可以冲红
     */
    public boolean canCredit() {
        return invoiceStatus != InvoiceStatus.CREDITED && 
               invoiceStatus != InvoiceStatus.CANCELLED &&
               invoiceStatus != InvoiceStatus.VOIDED &&
               !Boolean.TRUE.equals(isCreditNote);
    }
    
    /**
     * 检查是否可以作废
     */
    public boolean canVoid() {
        return invoiceStatus == InvoiceStatus.GENERATED && 
               paymentStatus == PaymentStatus.PENDING;
    }
    
    /**
     * 获取客户或供应商显示名称
     */
    public String getPartyDisplayName() {
        if (customerName != null) {
            return customerName;
        } else if (supplierName != null) {
            return supplierName;
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
        return invoiceStatus.getChineseName();
    }
    
    /**
     * 获取付款状态显示名称
     */
    public String getPaymentStatusDisplay() {
        return paymentStatus.getChineseName();
    }
    
    /**
     * 获取发票类型显示名称
     */
    public String getInvoiceTypeDisplay() {
        return invoiceType.getChineseName();
    }
    
    @Override
    public String getDisplayName() {
        return String.format("发票[%s] - %s", invoiceNumber, getPartyDisplayName());
    }
    
    @Override
    public String getEntityType() {
        return "INVOICE";
    }
}