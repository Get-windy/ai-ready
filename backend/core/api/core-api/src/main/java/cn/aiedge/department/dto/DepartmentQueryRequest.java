package cn.aiedge.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部门查询请求
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "部门查询请求")
public class DepartmentQueryRequest {

    @Schema(description = "部门编码")
    private String departmentCode;

    @Schema(description = "部门名称（模糊查询）")
    private String departmentName;

    @Schema(description = "父部门ID")
    private Long parentId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
