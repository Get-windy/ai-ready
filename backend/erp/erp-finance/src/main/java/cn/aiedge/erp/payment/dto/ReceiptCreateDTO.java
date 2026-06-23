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