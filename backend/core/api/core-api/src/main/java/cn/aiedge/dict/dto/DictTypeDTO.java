package cn.aiedge.dict.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 字典类型DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "字典类型DTO")
public class DictTypeDTO {

    @Schema(description = "字典类型ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @NotBlank(message = "字典类型编码不能为空")
    @Schema(description = "字典类型编码", required = true)
    private String dictCode;

    @NotBlank(message = "字典类型名称不能为空")
    @Schema(description = "字典类型名称", required = true)
    private String dictName;

    @Schema(description = "字典类型描述")
    private String description;

    @Schema(description = "父字典ID")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态：ENABLED, DISABLED")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "子字典类型列表")
    private List<DictTypeDTO> children;
}
