package cn.aiedge.position.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 岗位分类查询请求
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "岗位分类查询请求")
public class CategoryQueryRequest {

    @Schema(description = "分类名称（模糊查询）")
    private String categoryName;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
