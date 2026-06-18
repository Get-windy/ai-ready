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
@TableName("wms_ship_task")
@Schema(description = "发货复核任务")
public class WmsShipTask extends BaseEntity {
    @Schema(description = "任务单号")
    private String taskNo;
    @Schema(description = "关联拣货任务ID")
    private Long pickTaskId;
    @Schema(description = "关联出库单ID")
    private Long sourceOrderId;
    @Schema(description = "出库单号")
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
    @Schema(description = "已扫描数量")
    private BigDecimal scannedQuantity;
    @Schema(description = "状态 0-待复核 1-复核中 2-已发货 3-已取消")
    private Integer status;
    @Schema(description = "分配操作人")
    private Long assigneeId;
    @Schema(description = "操作人姓名")
    private String assigneeName;
    @Schema(description = "承运商")
    private String carrierName;
    @Schema(description = "运单号")
    private String trackingNo;
    @Schema(description = "发货时间")
    private LocalDateTime shipTime;
    @Schema(description = "备注")
    private String remark;
}
