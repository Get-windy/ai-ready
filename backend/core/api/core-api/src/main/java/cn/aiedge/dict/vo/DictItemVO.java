package cn.aiedge.dict.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典项VO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Schema(description = "字典项VO")
public class DictItemVO {

    @Schema(description = "字典项ID")
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "字典类型ID")
    private Long dictTypeId;

    @Schema(description = "字典项值")
    private String itemValue;

    @Schema(description = "字典项文本")
    private String itemText;

    @Schema(description = "字典项描述")
    private String description;

    @Schema(description = "父字典项ID")
    private Long parentId;

    @Schema(description = "排序号")
    private Integer sortOrder;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否默认")
    private String isDefault;

    @Schema(description = "扩展属性")
    private String extraAttrs;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子字典项列表")
    private List<DictItemVO> children;
}
