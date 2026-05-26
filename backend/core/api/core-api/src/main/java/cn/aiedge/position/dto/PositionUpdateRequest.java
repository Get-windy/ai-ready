package cn.aiedge.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 岗位更新请求
 */
@Data
@Schema(description = "岗位更新请求")
public class PositionUpdateRequest {

    @Schema(description = "岗位ID", required = true)
    @NotNull(message = "岗位ID不能为空")
    private Long id;

    @Schema(description = "岗位编码")
    @NotBlank(message = "岗位编码不能为空")
    private String positionCode;

    @Schema(description = "岗位名称")
    @NotBlank(message = "岗位名称不能为空")
    private String positionName;

    @Schema(description = "岗位分类ID")
    private Long categoryId;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "岗位职级")
    private Integer level;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "岗位描述")
    private String description;

    @Schema(description = "备注")
    private String remark;
}
