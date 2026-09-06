package cn.aiedge.wms.controller.dto;

import cn.aiedge.wms.entity.WmsPutawayDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 上架明细 VO（含主表 wms_putaway_task 字段，用于「按明细」分页展示）.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "上架明细VO（含任务主表字段）")
public class WmsPutawayDetailVO extends WmsPutawayDetail {

    @Schema(description = "任务单号")
    private String taskNo;

    @Schema(description = "来源类型 0-收货上架 1-退货上架 2-调拨上架 3-其他")
    private Integer sourceType;

    @Schema(description = "来源单据ID")
    private Long sourceId;

    @Schema(description = "来源单号")
    private String sourceOrderNo;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "任务总数量")
    private BigDecimal totalQuantity;

    @Schema(description = "已上架数量")
    private BigDecimal putawayQuantity;

    @Schema(description = "执行人ID")
    private Long assigneeId;

    @Schema(description = "执行人姓名")
    private String assigneeName;

    @Schema(description = "任务状态 0-待上架 1-上架中 2-已完成 3-已取消")
    private Integer taskStatus;

    @Schema(description = "任务创建时间")
    private LocalDateTime taskCreateTime;
}
