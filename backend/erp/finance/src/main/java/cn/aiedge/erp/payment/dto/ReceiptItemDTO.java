package cn.aiedge.erp.payment.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReceiptItemDTO {

    private Long orderId;

    private String orderNo;

    private Long invoiceId;

    private String invoiceNo;

    private BigDecimal orderAmount;

    private BigDecimal invoiceAmount;

    private BigDecimal pendingAmount;

    private BigDecimal verifyAmount;

    private String remark;
}