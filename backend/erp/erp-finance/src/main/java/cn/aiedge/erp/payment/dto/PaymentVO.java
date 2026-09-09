package cn.aiedge.erp.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PaymentVO {

    private Long id;

    private String paymentNo;

    private Integer paymentType;

    private Long supplierId;

    private String supplierName;

    private Long orderId;

    private String orderNo;

    private Long invoiceId;

    private String invoiceNo;

    private LocalDate paymentDate;

    private Integer status;

    private String statusDesc;

    private BigDecimal paymentAmount;

    private BigDecimal verifiedAmount;

    private BigDecimal pendingAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 使用预付款 */
    private BigDecimal usePrepaidAmount;

    /** 多付金额（付款超应付转供应商预付） */
    private BigDecimal overpayAmount;

    /** 此前应付 */
    private BigDecimal prevPayable;

    /** 应付余额 */
    private BigDecimal payableBalance;

    /** 预付款余额（可用预付） */
    private BigDecimal prepaidBalance;

    /** 付款账户1 */
    private String paymentAccount1;

    private BigDecimal paymentAmount1;

    /** 付款账户2 */
    private String paymentAccount2;

    private BigDecimal paymentAmount2;

    /** 付款账户3 */
    private String paymentAccount3;

    private BigDecimal paymentAmount3;

    /** 付款账户4 */
    private String paymentAccount4;

    private BigDecimal paymentAmount4;

    /** 结算单位编号（供应商主数据反查） */
    private String supplierCode;

    /** 制单人名称（用户表反查，当前实体仅存 createBy） */
    private String creatorName;

    /** 记账人名称（用户表反查，当前实体仅存 verifiedBy） */
    private String bookkeeperName;

    /** 审核人名称（用户表反查，当前实体仅存 approvedBy） */
    private String auditorName;

    /** 打印次数 */
    private Integer printCount;

    /** 摘要 */
    private String summary;

    private String paymentMethod;

    private String bankAccount;

    private String bankName;

    private String checkNo;

    private String transactionNo;

    private Long purchaserId;

    private String purchaserName;

    private Long departmentId;

    private String departmentName;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private Long verifiedBy;

    private LocalDateTime verifiedTime;

    private Long completedBy;

    private LocalDateTime completedTime;

    private String remark;

    private String internalNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private Integer versionNo;

    private List<?> items;
}