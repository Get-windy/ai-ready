package cn.aiedge.dms.task.dto;

import cn.aiedge.dms.task.entity.DmsTaskDoc;
import cn.aiedge.dms.task.entity.DmsTaskItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送单详情（头 + 商品明细）
 */
@Data
@Schema(description = "配送单详情")
public class DmsTaskDetailDTO {

    private Long id;

    private Long tenantId;

    private String taskNo;

    private LocalDate deliveryDate;

    private Integer orderType;

    private Integer priority;

    private String orderNo;

    private String sourceBillNo;

    private Long sourceWarehouseId;

    private String sourceAddress;

    private Long customerId;

    private String customerName;

    private String customerPhone;

    private String customerAddress;

    private LocalDateTime deadlineTime;

    private Long routeId;

    private String routeArea;

    private Long riderId;

    private String riderName;

    private Long vehicleId;

    private String vehicleName;

    private Long deliverymanId;

    private String deliverymanName;

    private BigDecimal totalQuantity;

    private BigDecimal totalWeight;

    private BigDecimal totalVolume;

    private BigDecimal goodsAmount;

    private BigDecimal deliveryFee;

    private BigDecimal collectOnDelivery;

    private BigDecimal estimatedDistance;

    private Integer orderCount;

    private BigDecimal depositAmount;

    private Integer returnOrderCount;

    private BigDecimal returnQuantity;

    private BigDecimal returnAmount;

    private BigDecimal boxQuantity;

    private Integer printCount;

    private String creatorName;

    private Integer status;

    private String remark;

    private LocalDateTime pickupTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime completedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Schema(description = "商品明细行（无上游单据的临时配送才由本单录入；有上游单据时明细由上游穿透查看）")
    private List<DmsTaskItem> items;

    @Schema(description = "来源上游单据（销售出库单等）——配送单表头数量/金额/重量/体积的聚合来源")
    private List<DmsTaskDoc> sourceDocs;
}
