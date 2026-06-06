package cn.aiedge.department.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 部门VO
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "部门信息")
public class DepartmentVO {

    @Schema(description = "部门ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "部门编码")
    private String departmentCode;

    @Schema(description = "部门名称")
    private String departmentName;

    @Schema(description = "父部门ID")
    private Long parentId;

    @Schema(description = "父部门名称")
    private String parentName;

    @Schema(description = "层级")
    private Integer level;

    @Schema(description = "祖级路径")
    private String path;

    @Schema(description = "部门负责人ID")
    private Long leaderId;

    @Schema(description = "部门负责人姓名")
    private String leaderName;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态 0-禁用 1-启用")
    private Integer status;

    @Schema(description = "子部门")
    private List<DepartmentVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
