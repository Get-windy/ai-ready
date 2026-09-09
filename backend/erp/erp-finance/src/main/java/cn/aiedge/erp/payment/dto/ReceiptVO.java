package cn.aiedge.erp.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReceiptVO {

    private Long id;

    private String receiptNo;

    private Integer receiptType;

    private Long customerId;

    private String customerName;

    private Long orderId;

    private String orderNo;

    private Long invoiceId;

    private String invoiceNo;

    private LocalDate receiptDate;

    private Integer status;

    private String statusDesc;

    private BigDecimal receiptAmount;

    private BigDecimal verifiedAmount;

    private BigDecimal pendingAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 使用预收款 */
    private BigDecimal usePrepaidAmount;

    /** 多收金额 */
    private BigDecimal overpayAmount;

    /** 此前应收 */
    private BigDecimal prevReceivable;

    /** 应收余额 */
    private BigDecimal receivableBalance;

    /** 预收余额（可用预收） */
    private BigDecimal prepaidBalance;

    /** 收款账户1 */
    private String receiptAccount1;

    private BigDecimal receiptAmount1;

    private String receiptAccount2;

    private BigDecimal receiptAmount2;

    private String receiptAccount3;

    private BigDecimal receiptAmount3;

    private String receiptAccount4;

    private BigDecimal receiptAmount4;

    private String paymentMethod;

    private String bankAccount;

    private String bankName;

    private String checkNo;

    private String transactionNo;

    private Long salesPersonId;

    private String salesPersonName;

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

    /** 制单人名称（待确认款项金额列展示，用户表反查。当前实体仅存 createBy） */
    private String creatorName;

    /** 摘要 */
    private String summary;

    /** 凭证（KJPZ-，确认记账后生成） */
    private String voucherNo;

    /** 配送任务编号（追溯至配送任务，当前数据链路拆分至配送模块） */
    private String deliveryNo;

    private List<?> items;
}