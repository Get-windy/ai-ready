package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应付账款DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayableDTO {
    private Long id;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private String supplierId;
    private String supplierName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private LocalDate dueDate;
    private LocalDate invoiceDate;
    private String invoiceNo;
    private String status;
    private String remark;
}
