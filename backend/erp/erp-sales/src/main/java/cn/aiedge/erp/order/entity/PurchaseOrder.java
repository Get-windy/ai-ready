package cn.aiedge.erp.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单实体
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("erp_purchase_order")
public class PurchaseOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 采购订单编号 */
    private String purchaseOrderNo;

    /** 供应商ID */
    private Long supplierId;

    /** 供应商名称 */
    private String supplierName;

    /** 采购员ID */
    private Long buyerId;

    /** 采购员名称 */
    private String buyerName;

    /** 采购类型：1-标准采购 2-紧急采购 3-框架协议采购 4-零星采购 */
    private Integer purchaseType;

    /** 订单来源：1-手动创建 2-需求计划 3-补货计划 4-其他 */
    private Integer source;

    /** 采购订单状态：
     * 0-草稿 1-提交待审 2-审批中 3-已批准 4-已拒绝 5-供应商确认中 
     * 6-已确认 7-发货中 8-部分收货 9-全部收货 10-待付款 11-部分付款 
     * 12-已付款 13-已完成 14-已取消 15-已关闭
     */
    private Integer status;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 折扣金额 */
    private BigDecimal discountAmount;

    /** 最终金额（含税） */
    private BigDecimal finalAmount;

    /** 货币类型 */
    private String currency;

    /** 汇率 */
    private BigDecimal exchangeRate;

    /** 交货地址 */
    private String deliveryAddress;

    /** 期望交货日期 */
    private LocalDateTime expectedDeliveryDate;

    /** 实际交货日期 */
    private LocalDateTime actualDeliveryDate;

    /** 付款条件 */
    private Integer paymentTerms;

    /** 付款条件描述 */
    private String paymentTermsDesc;

    /** 付款到期日 */
    private LocalDateTime paymentDueDate;

    /** 国际贸易术语 */
    private String incoterms;

    /** 运输方式 */
    private String shippingMethod;

    /** 审批级别 */
    private Integer approvalLevel;

    /** 当前审批人ID */
    private Long currentApproverId;

    /** 当前审批人名称 */
    private String currentApproverName;

    /** 提交审批时间 */
    private LocalDateTime submitForApprovalTime;

    /** 审批完成时间 */
    private LocalDateTime approvalCompleteTime;

    /** 供应商确认编号 */
    private String supplierConfirmationNo;

    /** 供应商确认时间 */
    private LocalDateTime supplierConfirmationTime;

    /** 供应商确认备注 */
    private String supplierConfirmationRemark;

    /** 物流跟踪号 */
    private String trackingNo;

    /** 物流公司 */
    private String logisticsCompany;

    /** 预计到达时间 */
    private LocalDateTime estimatedArrivalDate;

    /** 实际到达时间 */
    private LocalDateTime actualArrivalDate;

    /** 已收货数量 */
    private Integer receivedQuantity;

    /** 已收货金额 */
    private BigDecimal receivedAmount;

    /** 质量检查结果 */
    private String qualityCheckResult;

    /** 发票编号 */
    private String invoiceNo;

    /** 发票金额 */
    private BigDecimal invoiceAmount;

    /** 发票日期 */
    private LocalDateTime invoiceDate;

    /** 已付款金额 */
    private BigDecimal paidAmount;

    /** 付款状态：0-未付款 1-部分付款 2-已付款 */
    private Integer paymentStatus;

    /** 付款完成时间 */
    private LocalDateTime paymentCompleteTime;

    /** 订单完成时间 */
    private LocalDateTime orderCompleteTime;

    /** 取消原因 */
    private String cancelReason;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 关闭原因 */
    private String closeReason;

    /** 关闭时间 */
    private LocalDateTime closeTime;

    /** 备注 */
    private String remark;

    /** 扩展信息 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String extInfo;

    /** 是否删除 */
    @TableLogic
    private Integer deleted;

    /** 版本号（乐观锁） */
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 采购订单状态枚举
     */
    public enum Status {
        DRAFT(0, "草稿"),
        SUBMITTED(1, "提交待审"),
        APPROVING(2, "审批中"),
        APPROVED(3, "已批准"),
        REJECTED(4, "已拒绝"),
        SUPPLIER_CONFIRMING(5, "供应商确认中"),
        CONFIRMED(6, "已确认"),
        SHIPPING(7, "发货中"),
        PARTIALLY_RECEIVED(8, "部分收货"),
        FULLY_RECEIVED(9, "全部收货"),
        PENDING_PAYMENT(10, "待付款"),
        PARTIALLY_PAID(11, "部分付款"),
        PAID(12, "已付款"),
        COMPLETED(13, "已完成"),
        CANCELLED(14, "已取消"),
        CLOSED(15, "已关闭");

        private final int code;
        private final String description;

        Status(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static Status fromCode(int code) {
            for (Status status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
            return DRAFT;
        }
    }

    /**
     * 采购类型枚举
     */
    public enum PurchaseType {
        STANDARD(1, "标准采购"),
        EMERGENCY(2, "紧急采购"),
        FRAMEWORK(3, "框架协议采购"),
        SPOT(4, "零星采购");

        private final int code;
        private final String description;

        PurchaseType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 付款状态枚举
     */
    public enum PaymentStatus {
        UNPAID(0, "未付款"),
        PARTIALLY_PAID(1, "部分付款"),
        PAID(2, "已付款");

        private final int code;
        private final String description;

        PaymentStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}