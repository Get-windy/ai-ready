package cn.aiedge.erp.fixedasset.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产处置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fixed_asset_disposal", indexes = {
    @Index(name = "idx_fadisposal_no", columnList = "disposal_no"),
    @Index(name = "idx_fadisposal_asset_id", columnList = "asset_id"),
    @Index(name = "idx_fadisposal_asset_code", columnList = "asset_code"),
    @Index(name = "idx_fadisposal_status", columnList = "status")
})
public class FixedAssetDisposal extends BaseEntity {

    @Column(name = "disposal_no", length = 50)
    private String disposalNo;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "asset_code", length = 50)
    private String assetCode;

    @Column(name = "asset_name", length = 200)
    private String assetName;

    @Column(name = "disposal_date")
    private LocalDate disposalDate;

    @Column(name = "disposal_type", length = 20)
    private String disposalType;

    @Column(name = "disposal_amount", precision = 15, scale = 2)
    private BigDecimal disposalAmount;

    @Column(name = "net_value", precision = 15, scale = 2)
    private BigDecimal netValue;

    @Column(name = "gain_loss", precision = 15, scale = 2)
    private BigDecimal gainLoss;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", length = 20)
    private String status = "draft";

    @Column(name = "approval_comment", length = 500)
    private String approvalComment;
}
