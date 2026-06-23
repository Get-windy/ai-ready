package cn.aiedge.erp.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 凭证明细行DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherItemDTO {
    private Long id;
    private Long voucherId;
    private String summary;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private String remark;
}
