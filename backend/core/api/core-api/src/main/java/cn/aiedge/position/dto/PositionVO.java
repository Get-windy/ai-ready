package cn.aiedge.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位详情VO
 */
@Data
@Schema(description = "岗位详情")
public class PositionVO {

    @Schema(description = "岗位ID")
    private Long id;

    @Schema(description = "岗位编码")
    private String positionCode;

    @Schema(description = "岗位名称")
    private String positionName;

    @Schema(description = "岗位分类ID")
    private Long categoryId;

    @Schema(description = "岗位分类名称")
    private String categoryName;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    private String deptName;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "关联用户数")
    private Integer userCount;
}
