package cn.aiedge.erp.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PaymentCreateDTO {

    private Integer paymentType;

    private Long supplierId;

    private String supplierName;

    private Long orderId;

    private String orderNo;

    private Long invoiceId;

    private String invoiceNo;

    private LocalDate paymentDate;

    private BigDecimal paymentAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 使用预付款 */
    private BigDecimal usePrepaidAmount;

    /** 多付金额 */
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

    private String paymentAccount2;

    private BigDecimal paymentAmount2;

    private String paymentAccount3;

    private BigDecimal paymentAmount3;

    private String paymentAccount4;

    private BigDecimal paymentAmount4;

    /** 来源业务编号 */
    private String sourceNo;

    private String paymentMethod;

    private String bankAccount;

    private String bankName;

    private String checkNo;

    private String transactionNo;

    private Long purchaserId;

    private String purchaserName;

    private Long departmentId;

    private String departmentName;

    private String remark;

    private String internalNote;

    private List<PaymentItemDTO> items;
}