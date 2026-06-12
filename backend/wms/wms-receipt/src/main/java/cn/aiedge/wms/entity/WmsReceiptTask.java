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
@TableName("wms_receipt_task")
@Schema(description = "收货任务")
public class WmsReceiptTask extends BaseEntity {
    @Schema(description = "任务单号")
    private String taskNo;
    @Schema(description = "来源类型 1-采购入库 2-退货入库 3-调拨入库 4-盘盈入库")
    private Integer sourceType;
    @Schema(description = "来源单据ID")
    private Long sourceOrderId;
    @Schema(description = "来源单据号")
    private String sourceOrderNo;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "供应商ID")
    private Long supplierId;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "总商品数")
    private Integer totalItems;
    @Schema(description = "总应收数量")
    private BigDecimal totalQuantity;
    @Schema(description = "已收数量")
    private BigDecimal receivedQuantity;
    @Schema(description = "状态 0-待收货 1-收货中 2-已完成 3-已取消 4-异常")
    private Integer status;
    @Schema(description = "优先级 1-普通 2-紧急 3-加急")
    private Integer priority;
    @Schema(description = "预期到货时间")
    private LocalDateTime expectedTime;
    @Schema(description = "完成时间")
    private LocalDateTime completedTime;
    @Schema(description = "分配操作人")
    private Long assigneeId;
    @Schema(description = "操作人姓名")
    private String assigneeName;
    @Schema(description = "备注")
    private String remark;
}
