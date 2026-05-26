package cn.aiedge.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 字典项DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "字典项DTO")
public class DictItemDTO {

    @Schema(description = "字典项ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @NotNull(message = "字典类型ID不能为空")
    @Schema(description = "字典类型ID", required = true)
    private Long dictTypeId;

    @NotBlank(message = "字典项值不能为空")
    @Schema(description = "字典项值", required = true)
    private String itemValue;

    @NotBlank(message = "字典项文本不能为空")
    @Schema(description = "字典项文本", required = true)
    private String itemText;

    @Schema(description = "字典项描述")
    private String description;

    @Schema(description = "父字典项ID")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：ENABLED, DISABLED")
    private String status;

    @Schema(description = "是否默认：Y-是, N-否")
    private String isDefault;

    @Schema(description = "扩展属性（JSON格式）")
    private String extraAttrs;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "子字典项列表")
    private List<DictItemDTO> children;
}
