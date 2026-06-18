package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inventory_log")
@Schema(description = "库存异动日志")
public class WmsInventoryLog extends BaseEntity {
    @Schema(description = "链路追踪ID")
    private String traceId;
    @Schema(description = "商品ID")
    private Long productId;
    @Schema(description = "商品编码")
    private String productCode;
    @Schema(description = "商品名称")
    private String productName;
    @Schema(description = "仓库ID")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "货位ID")
    private Long locationId;
    @Schema(description = "货位编码")
    private String locationCode;
    @Schema(description = "批次号")
    private String batchNo;
    @Schema(description = "序列号")
    private String serialNo;
    @Schema(description = "变动类型 1-入库 2-出库 3-冻结 4-解冻 5-盘盈 6-盘亏 7-移库 8-调整")
    private Integer changeType;
    @Schema(description = "方向 1-入库(增加) -1-出库(减少)")
    private Integer direction;
    @Schema(description = "变动数量")
    private BigDecimal quantity;
    @Schema(description = "变动前数量")
    private BigDecimal beforeQuantity;
    @Schema(description = "变动后数量")
    private BigDecimal afterQuantity;
    @Schema(description = "来源单据类型")
    private String sourceType;
    @Schema(description = "来源单据ID")
    private Long sourceId;
    @Schema(description = "来源单据号")
    private String sourceNo;
    @Schema(description = "操作人ID")
    private Long operatorId;
    @Schema(description = "操作人姓名")
    private String operatorName;
    @Schema(description = "备注")
    private String remark;
}
