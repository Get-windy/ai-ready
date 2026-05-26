package cn.aiedge.dict.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典类型VO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "字典类型VO")
public class DictTypeVO {

    @Schema(description = "字典类型ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "字典类型编码")
    private String dictCode;

    @Schema(description = "字典类型名称")
    private String dictName;

    @Schema(description = "字典类型描述")
    private String description;

    @Schema(description = "父字典ID")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否内置")
    private String isBuiltIn;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子字典类型列表")
    private List<DictTypeVO> children;

    @Schema(description = "字典项列表")
    private List<DictItemVO> items;
}
