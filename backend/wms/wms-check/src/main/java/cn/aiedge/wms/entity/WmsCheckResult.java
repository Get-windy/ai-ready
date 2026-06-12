package cn.aiedge.wms.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_check_result")
@Schema(description = "盘点结果")
public class WmsCheckResult extends BaseEntity {
    @Schema(description = "关联盘点任务ID")
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
    @Schema(description = "货位ID")
    private Long locationId;
    @Schema(description = "货位编码")
    private String locationCode;
    @Schema(description = "批次号")
    private String batchNo;
    @Schema(description = "账面数量")
    private BigDecimal bookQuantity;
    @Schema(description = "实盘数量")
    private BigDecimal actualQuantity;
    @Schema(description = "差异数量")
    private BigDecimal diffQuantity;
    @Schema(description = "差异类型 0-正常 1-盘盈 2-盘亏")
    private Integer diffType;
    @Schema(description = "单位成本")
    private BigDecimal unitCost;
    @Schema(description = "差异金额")
    private BigDecimal diffAmount;
    @Schema(description = "状态 0-待盘点 1-已盘点 2-已确认 3-已调整")
    private Integer checkStatus;
    @Schema(description = "备注")
    private String remark;
}
