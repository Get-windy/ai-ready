package cn.aiedge.erp.purchase.purchasereturn.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseReturnDTO {

    private Long id;

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

    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private String extText1;
    private String extText2;
    private String extText3;

    private String summary;

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

    private String reason;

    /** 单据状态（0草稿 1提交审批），用于创建时直接提交 */
    private Integer status;

    private List<PurchaseReturnItemDTO> items;
}
