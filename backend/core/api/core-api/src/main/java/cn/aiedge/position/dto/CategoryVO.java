package cn.aiedge.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位分类VO
 */
@Data
@Schema(description = "岗位分类")
public class CategoryVO {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "父分类ID")
    private Long parentId;

    @Schema(description = "祖级列表")
    private String ancestors;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子分类")
    private List<CategoryVO> children;
}
