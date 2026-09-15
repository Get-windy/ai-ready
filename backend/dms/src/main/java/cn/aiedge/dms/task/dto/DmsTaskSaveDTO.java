package cn.aiedge.dms.task.dto;

import cn.aiedge.dms.task.entity.DmsTaskDoc;
import cn.aiedge.dms.task.entity.DmsTaskItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送单保存（头 + 商品明细）
 */
@Data
@Schema(description = "配送单保存入参")
public class DmsTaskSaveDTO {

    @Schema(description = "主键；为空表示新增")
    private Long id;

    @Schema(description = "任务编号（为空由后端号段生成）")
    private String taskNo;

    @Schema(description = "指定配送日期")
    private LocalDate deliveryDate;

    @Schema(description = "订单类型 1-销售配送 2-调拨 3-退货")
    private Integer orderType;

    @Schema(description = "优先级 1-普通 2-紧急 3-加急")
    private Integer priority;

    @NotBlank(message = "关联订单号不能为空")
    @Schema(description = "关联订单号")
    private String orderNo;

    @Schema(description = "来源单据编号")
    private String sourceBillNo;

    @Schema(description = "来源仓库ID")
    private Long sourceWarehouseId;

    @Schema(description = "发货地址")
    private String sourceAddress;

    @NotBlank(message = "客户名称不能为空")
    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "客户ID（往来单位）")
    private Long customerId;

    @Schema(description = "联系电话")
    private String customerPhone;

    @Schema(description = "收货地址")
    private String customerAddress;

    @Schema(description = "要求送达时间")
    private LocalDateTime deadlineTime;

    @Schema(description = "配送线路ID")
    private Long routeId;

    @Schema(description = "配送区域")
    private String routeArea;

    @Schema(description = "配送司机ID")
    private Long riderId;

    @Schema(description = "配送司机名称")
    private String riderName;

    @Schema(description = "配送车辆ID")
    private Long vehicleId;

    @Schema(description = "配送车辆名称（车牌号）")
    private String vehicleName;

    @Schema(description = "送货员ID（dms_rider.id）")
    private Long deliverymanId;

    @Schema(description = "送货员名称")
    private String deliverymanName;

    @Schema(description = "配送费")
    private BigDecimal deliveryFee;

    @Schema(description = "订金金额")
    private BigDecimal depositAmount;

    @Schema(description = "装箱数量")
    private BigDecimal boxQuantity;

    @Schema(description = "代收货款")
    private BigDecimal collectOnDelivery;

    @Schema(description = "配送里程(km)")
    private BigDecimal estimatedDistance;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "制单人名称")
    private String creatorName;

    @Schema(description = "商品明细（无上游单据的临时配送使用；有上游单据时明细由上游穿透，本字段忽略）")
    private List<DmsTaskItem> items;

    @Schema(description = "来源上游单据（销售出库单等）：选中后表头发货数量/金额/重量/体积由上游聚合，配送单不再手工录入货物金额")
    private List<DmsTaskDoc> sourceDocs;
}
