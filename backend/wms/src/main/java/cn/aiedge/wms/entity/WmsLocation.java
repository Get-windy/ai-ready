package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_location")
@Schema(description = "库区/货位")
public class WmsLocation extends BaseEntity {
    @Schema(description = "关联仓库ID")
    private Long warehouseId;
    @Schema(description = "货位编码（层级，如 A-01-01）")
    private String locationCode;
    @Schema(description = "货位名称")
    private String locationName;
    @Schema(description = "类型 1-存储位 2-暂存位 3-拣货位 4-收货区 5-发货区 6-报废区")
    private Integer locationType;
    @Schema(description = "层级 1-库区 2-巷道 3-货架 4-货位")
    private Integer locationLevel;
    @Schema(description = "父级ID")
    private Long parentId;
    @Schema(description = "层级路径")
    private String path;
    @Schema(description = "最大容量")
    private BigDecimal maxCapacity;
    @Schema(description = "已用容量")
    private BigDecimal usedCapacity;
    @Schema(description = "最大载重(kg)")
    private BigDecimal maxWeight;
    @Schema(description = "长(cm)")
    private BigDecimal lengthCm;
    @Schema(description = "宽(cm)")
    private BigDecimal widthCm;
    @Schema(description = "高(cm)")
    private BigDecimal heightCm;
    @Schema(description = "状态 1-空闲 2-占用 3-冻结 4-维修")
    private Integer status;
    @Schema(description = "是否可拣货 0-否 1-是")
    private Integer isPickable;
    @Schema(description = "是否可收货 0-否 1-是")
    private Integer isReceivable;
    @Schema(description = "排序")
    private Integer sortOrder;
    @Schema(description = "备注")
    private String remark;
}
