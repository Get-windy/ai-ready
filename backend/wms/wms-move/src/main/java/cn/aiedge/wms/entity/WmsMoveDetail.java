package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_move_detail")
@Schema(description = "移库明细")
public class WmsMoveDetail extends BaseEntity {
    @Schema(description = "关联移库任务ID")
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
    @Schema(description = "移库数量")
    private BigDecimal quantity;
    @Schema(description = "批次号")
    private String batchNo;
    @Schema(description = "序列号")
    private String serialNo;
    @Schema(description = "源货位ID")
    private Long fromLocationId;
    @Schema(description = "源货位编码")
    private String fromLocationCode;
    @Schema(description = "目标货位ID")
    private Long toLocationId;
    @Schema(description = "目标货位编码")
    private String toLocationCode;
    @Schema(description = "状态 0-待移库 1-已移库")
    private Integer status;
    @Schema(description = "备注")
    private String remark;
}
