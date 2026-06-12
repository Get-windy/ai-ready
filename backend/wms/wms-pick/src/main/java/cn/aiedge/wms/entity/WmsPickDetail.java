package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_pick_detail")
@Schema(description = "拣货明细")
public class WmsPickDetail extends BaseEntity {
    @Schema(description = "关联拣货任务ID")
    private Long taskId;
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
    @Schema(description = "拣货货位ID")
    private Long locationId;
    @Schema(description = "拣货货位编码")
    private String locationCode;
    @Schema(description = "应拣数量")
    private BigDecimal expectedQuantity;
    @Schema(description = "实拣数量")
    private BigDecimal pickedQuantity;
    @Schema(description = "缺货数量")
    private BigDecimal shortageQuantity;
    @Schema(description = "批次号")
    private String batchNo;
    @Schema(description = "序列号")
    private String serialNo;
    @Schema(description = "状态 0-待拣货 1-已拣货 2-缺货")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
