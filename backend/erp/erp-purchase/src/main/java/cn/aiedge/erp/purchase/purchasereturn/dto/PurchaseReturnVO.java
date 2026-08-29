package cn.aiedge.erp.purchase.purchasereturn.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseReturnVO {

    private Long id;

    private String returnNo;

    private Long purchaseOrderId;

    private String purchaseOrderNo;

    private Long supplierId;

    private String supplierName;

    private String supplierNo;

    private String bankName;

    private String bankAccount;

    private String taxNo;

    private Long warehouseId;

    private String warehouseName;

    private Long purchaserId;

    private String purchaserName;

    private Long departmentId;

    private String departmentName;

    private LocalDate returnDate;

    private String contactName;

    private String contactPhone;

    private String contactAddress;

    private String supplierRemark;

    private Integer returnType;

    private String returnTypeDesc;

    private BigDecimal totalQuantity;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal taxAmount;

    private BigDecimal totalAmountWithTax;

    private BigDecimal settledAmount;

    private Integer settleStatus;

    private BigDecimal weight;

    private BigDecimal volume;

    private String summary;

    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private String extText1;
    private String extText2;
    private String extText3;

    private String paymentAccount;
    private BigDecimal paymentAmount;
    private String moreAccounts;
    private BigDecimal prevPrepaid;
    private BigDecimal refundPrepay;
    private BigDecimal prepaidBalance;
    private BigDecimal currentDebt;
    private BigDecimal prevDebt;
    private BigDecimal debtBalance;
    private LocalDate paymentDeadline;

    private String remark;

    private String createByName;

    private String posterName;

    private LocalDateTime postTime;

    private String attachment;

    private Integer printCount;

    private Integer status;

    private String statusDesc;

    private Long applicantId;

    private String applicantName;

    private LocalDateTime applyTime;

    private Long approvedBy;

    private LocalDateTime approvedTime;

    private String approvedNote;

    private String reason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;

    private List<?> items;
}
