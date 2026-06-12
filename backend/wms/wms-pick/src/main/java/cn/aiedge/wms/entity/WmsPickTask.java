package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_pick_task")
@Schema(description = "拣货任务")
public class WmsPickTask extends BaseEntity {
    @Schema(description = "任务单号")
    private String taskNo;
    @Schema(description = "关联波次ID")
    private Long waveId;
    @Schema(description = "来源 1-销售出库 2-退货出库 3-调拨出库 4-盘亏出库")
    private Integer sourceType;
    @Schema(description = "来源单据ID")
    private Long sourceOrderId;
    @Schema(description = "来源单据号")
    private String sourceOrderNo;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "客户ID")
    private Long customerId;
    @Schema(description = "客户名称")
    private String customerName;
    @Schema(description = "总商品数")
    private Integer totalItems;
    @Schema(description = "总数量")
    private BigDecimal totalQuantity;
    @Schema(description = "已拣货数量")
    private BigDecimal pickedQuantity;
    @Schema(description = "状态 0-待拣货 1-拣货中 2-已完成 3-缺货 4-已取消")
    private Integer status;
    @Schema(description = "优先级")
    private Integer priority;
    @Schema(description = "分配操作人")
    private Long assigneeId;
    @Schema(description = "操作人姓名")
    private String assigneeName;
    @Schema(description = "期望发货时间")
    private LocalDateTime expectShipTime;
    @Schema(description = "备注")
    private String remark;
}
