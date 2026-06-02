package cn.aiedge.erp.fixedasset.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产折旧记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fixed_asset_depreciation", indexes = {
    @Index(name = "idx_fad_asset_id", columnList = "asset_id"),
    @Index(name = "idx_fad_period", columnList = "period"),
    @Index(name = "idx_fad_asset_code", columnList = "asset_code")
})
public class FixedAssetDepreciation extends BaseEntity {

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "period", nullable = false, length = 7)
    private String period;

    @Column(name = "depreciation_date")
    private LocalDate depreciationDate;

    @Column(name = "period_amount", precision = 15, scale = 2)
    private BigDecimal periodAmount;

    @Column(name = "accumulated_depreciation", precision = 15, scale = 2)
    private BigDecimal accumulatedDepreciation;

    @Column(name = "net_value", precision = 15, scale = 2)
    private BigDecimal netValue;

    @Column(name = "asset_original_value", precision = 15, scale = 2)
    private BigDecimal assetOriginalValue;

    @Column(name = "asset_name", length = 200)
    private String assetName;

    @Column(name = "asset_code", length = 50)
    private String assetCode;

    @Column(name = "status", length = 20)
    private String status;
}
