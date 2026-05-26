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