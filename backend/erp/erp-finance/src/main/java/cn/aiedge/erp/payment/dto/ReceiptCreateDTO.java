package cn.aiedge.erp.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReceiptCreateDTO {

    private Integer receiptType;

    private Long customerId;

    private String customerName;

    private Long orderId;

    private String orderNo;

    private Long invoiceId;

    private String invoiceNo;

    private LocalDate receiptDate;

    private BigDecimal receiptAmount;

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

    private String remark;

    private String internalNote;

    private List<ReceiptItemDTO> items;
}