package cn.aiedge.erp.fixedasset.dto;

import lombok.Data;

/**
 * 固定资产分类DTO
 */
@Data
public class FixedAssetCategoryDTO {
    private Long id;
    private String categoryCode;
    private String categoryName;
    private Long parentId;
    private Integer sortOrder;
    private String defaultDepreciationMethod;
    private Integer defaultUsefulLife;
    private String description;
    private String remark;
}
