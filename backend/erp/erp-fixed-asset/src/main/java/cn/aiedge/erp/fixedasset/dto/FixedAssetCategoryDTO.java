package cn.aiedge.erp.fixedasset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 固定资产分类DTO
 */
@Data
@Schema(description = "固定资产分类")
public class FixedAssetCategoryDTO {

    @Schema(description = "ID")
    private Long id;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 50, message = "分类编码长度不能超过50")
    @Schema(description = "分类编码")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过100")
    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "父分类ID")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "默认折旧方法")
    private String defaultDepreciationMethod;

    @Schema(description = "默认使用年限（月）")
    private Integer defaultUsefulLife;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "描述")
    private String description;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
}
