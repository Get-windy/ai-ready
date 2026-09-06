package cn.aiedge.wms.ship.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发货明细分页视图（按明细分页，JOIN 发货单头补冗余信息）.
 */
@Data
@Schema(description = "发货明细分页视图")
public class WmsShipDetailPageVO {

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "发货任务ID")
    private Long shipId;

    @Schema(description = "行号")
    private Integer lineNo;

    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "商品编码")
    private String productCode;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品规格")
    private String productSpec;

    @Schema(description = "单位")
    private String productUnit;

    @Schema(description = "应发数量")
    private BigDecimal expectedQuantity;

    @Schema(description = "扫描数量")
    private BigDecimal scannedQuantity;

    @Schema(description = "确认数量")
    private BigDecimal confirmedQuantity;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "序列号")
    private String serialNo;

    @Schema(description = "货位ID")
    private Long locationId;

    @Schema(description = "货位编码")
    private String locationCode;

    @Schema(description = "明细状态 0-待扫描 1-已扫描 2-已确认")
    private Integer status;

    @Schema(description = "明细备注")
    private String remark;

    // ── 单头冗余字段 ──
    @Schema(description = "任务单号")
    private String taskNo;

    @Schema(description = "来源单号")
    private String sourceOrderNo;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "承运商")
    private String carrierName;

    @Schema(description = "运单号")
    private String trackingNo;

    @Schema(description = "单据状态 0-待复核 1-复核中 2-已发货 3-已取消")
    private Integer taskStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
