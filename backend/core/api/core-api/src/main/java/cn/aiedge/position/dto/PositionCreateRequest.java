package cn.aiedge.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 岗位创建请求
 */
@Data
@Schema(description = "岗位创建请求")
public class PositionCreateRequest {

    @Schema(description = "岗位编码", required = true)
    @NotBlank(message = "岗位编码不能为空")
    private String positionCode;

    @Schema(description = "岗位名称", required = true)
    @NotBlank(message = "岗位名称不能为空")
    private String positionName;

    @Schema(description = "岗位分类ID")
    private Long categoryId;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "岗位职级", example = "1")
    private Integer level;

    @Schema(description = "排序号", example = "0")
    private Integer sort;

    @Schema(description = "状态：0-禁用，1-启用", example = "1")
    private Integer status;

    @Schema(description = "岗位描述")
    private String description;

    @Schema(description = "备注")
    private String remark;
}
