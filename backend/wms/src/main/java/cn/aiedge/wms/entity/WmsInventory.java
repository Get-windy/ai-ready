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
@TableName("wms_inventory")
@Schema(description = "实时库存（WMS明细维度）")
public class WmsInventory extends BaseEntity {
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
    @Schema(description = "总数量")
    private BigDecimal quantity;
    @Schema(description = "可用量")
    private BigDecimal availableQuantity;
    @Schema(description = "冻结量")
    private BigDecimal frozenQuantity;
    @Schema(description = "单位成本")
    private BigDecimal unitCost;
    @Schema(description = "供应商ID")
    private Long supplierId;
    @Schema(description = "供应商名称")
    private String supplierName;
    @Schema(description = "生产日期")
    private LocalDateTime productionDate;
    @Schema(description = "有效期至")
    private LocalDateTime validityDate;
    @Schema(description = "备注")
    private String remark;
}
