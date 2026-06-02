package cn.aiedge.erp.fixedasset.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;

/**
 * 固定资产分类实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "fixed_asset_category", indexes = {
    @Index(name = "idx_fac_category_code", columnList = "category_code"),
    @Index(name = "idx_fac_parent_id", columnList = "parent_id")
})
public class FixedAssetCategory extends BaseEntity {

    @Column(name = "category_code", length = 50)
    private String categoryCode;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "default_depreciation_method", length = 30)
    private String defaultDepreciationMethod;

    @Column(name = "default_useful_life")
    private Integer defaultUsefulLife;

    @Column(name = "description", length = 500)
    private String description;
}
