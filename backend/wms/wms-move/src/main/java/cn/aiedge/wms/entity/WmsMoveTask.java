package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_move_task")
@Schema(description = "移库任务")
public class WmsMoveTask extends BaseEntity {
    @Schema(description = "任务单号")
    private String taskNo;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "源货位ID")
    private Long fromLocationId;
    @Schema(description = "源货位编码")
    private String fromLocationCode;
    @Schema(description = "目标货位ID")
    private Long toLocationId;
    @Schema(description = "目标货位编码")
    private String toLocationCode;
    @Schema(description = "总商品数")
    private Integer totalItems;
    @Schema(description = "总数量")
    private BigDecimal totalQuantity;
    @Schema(description = "已移库数量")
    private BigDecimal movedQuantity;
    @Schema(description = "状态 0-待移库 1-移库中 2-已完成 3-已取消")
    private Integer status;
    @Schema(description = "移库类型 1-库内移库 2-补货移库 3-整理移库")
    private Integer moveType;
    @Schema(description = "分配操作人")
    private Long assigneeId;
    @Schema(description = "操作人姓名")
    private String assigneeName;
    @Schema(description = "备注")
    private String remark;
}
