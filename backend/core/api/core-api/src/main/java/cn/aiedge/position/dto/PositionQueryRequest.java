package cn.aiedge.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 岗位查询请求
 */
@Data
@Schema(description = "岗位查询请求")
public class PositionQueryRequest {

    @Schema(description = "岗位编码")
    private String positionCode;

    @Schema(description = "岗位名称（模糊查询）")
    private String positionName;

    @Schema(description = "岗位分类ID")
    private Long categoryId;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
