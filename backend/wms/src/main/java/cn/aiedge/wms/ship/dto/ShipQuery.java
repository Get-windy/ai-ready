package cn.aiedge.wms.ship.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发货单列表查询参数（按单据 / 按明细共用）.
 * <p>对齐报损单 {@code StockDamageQuery} 风格，使用 pageNum/pageSize。</p>
 */
@Data
@Schema(description = "发货单查询参数")
public class ShipQuery {

    @Schema(description = "页码")
    private Long pageNum = 1L;

    @Schema(description = "每页条数")
    private Long pageSize = 20L;

    @Schema(description = "关键字（单号/来源单号模糊）")
    private String keyword;

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
    private Integer status;

    @Schema(description = "商品名称（按明细查询）")
    private String productName;

    @Schema(description = "商品编码（按明细查询）")
    private String productCode;

    @Schema(description = "创建时间起（yyyy-MM-dd）")
    private String startDate;

    @Schema(description = "创建时间止（yyyy-MM-dd）")
    private String endDate;
}
