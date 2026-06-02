package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 记账凭证DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDTO {
    private Long id;
    private String voucherNo;
    private LocalDate voucherDate;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private Integer attachments;
    private String prepBy;
    private LocalDateTime prepAt;
    private String auditBy;
    private LocalDateTime auditAt;
    private String postBy;
    private LocalDateTime postAt;
    private String status;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private String remark;
    private List<VoucherItemDTO> items;
}
