package cn.aiedge.wms.move.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 移库明细视图对象（按明细 Tab：明细字段 + 单据级字段）.
 * <p>对齐报损单 StockDamageItemVO：列表「按明细」时为明细行补齐单头字段以便展示。</p>
 */
@Data
@Schema(description = "移库明细视图对象")
public class MoveDetailVO {

    // ── 明细字段 ──
    @Schema(description = "明细ID")
    private Long id;
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
    @Schema(description = "明细状态 0待移库 1已移库")
    private Integer itemStatus;
    @Schema(description = "明细备注")
    private String itemRemark;

    // ── 单据级字段 ──
    @Schema(description = "任务单号")
    private String taskNo;
    @Schema(description = "仓库名称")
    private String warehouseName;
    @Schema(description = "单据状态 0待移库 1移库中 2已完成 3已取消")
    private Integer status;
    @Schema(description = "移库类型 1库内 2补货 3整理")
    private Integer moveType;
    @Schema(description = "来源单号")
    private String sourceNo;
    @Schema(description = "执行人")
    private String assigneeName;
    @Schema(description = "单据备注")
    private String docRemark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
