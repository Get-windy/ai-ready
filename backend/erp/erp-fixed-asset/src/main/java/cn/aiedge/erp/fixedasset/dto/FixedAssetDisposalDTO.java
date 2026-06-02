package cn.aiedge.erp.fixedasset.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产处置DTO
 */
@Data
public class FixedAssetDisposalDTO {
    private Long id;
    private String disposalNo;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private LocalDate disposalDate;
    private String disposalType;
    private BigDecimal disposalAmount;
    private BigDecimal netValue;
    private BigDecimal gainLoss;
    private String reason;
    private String status;
    private String approvalComment;
    private String remark;
}
