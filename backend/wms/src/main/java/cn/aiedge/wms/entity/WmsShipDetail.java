package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_ship_detail")
@Schema(description = "发货复核明细")
public class WmsShipDetail extends BaseEntity {
    @Schema(description = "关联发货任务ID")
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
    @Schema(description = "状态 0-待扫描 1-已扫描 2-已确认")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
