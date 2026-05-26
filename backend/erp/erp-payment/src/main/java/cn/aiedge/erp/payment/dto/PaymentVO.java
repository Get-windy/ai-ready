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