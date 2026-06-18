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
@TableName("wms_putaway_detail")
@Schema(description = "上架明细")
public class WmsPutawayDetail extends BaseEntity {
    @Schema(description = "关联上架任务ID")
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
    @Schema(description = "上架数量")
    private BigDecimal quantity;
    @Schema(description = "源货位ID")
    private Long fromLocationId;
    @Schema(description = "源货位编码")
    private String fromLocationCode;
    @Schema(description = "目标货位ID")
    private Long toLocationId;
    @Schema(description = "目标货位编码")
    private String toLocationCode;
    @Schema(description = "批次号")
    private String batchNo;
    @Schema(description = "生产日期")
    private LocalDateTime productionDate;
    @Schema(description = "有效期至")
    private LocalDateTime validityDate;
    @Schema(description = "状态 0-待上架 1-已上架")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
