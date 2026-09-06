package cn.aiedge.wms.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 上架明细查询（按明细分页：单据级过滤 + 明细级过滤）.
 */
@Data
@Schema(description = "上架明细分页查询")
public class WmsPutawayDetailQuery implements Serializable {

    @Schema(description = "页码，从1开始")
    private Integer pageNum = 1;

    @Schema(description = "每页条数")
    private Integer pageSize = 20;

    // ── 单据级过滤 ──
    @Schema(description = "单号模糊")
    private String taskNo;

    @Schema(description = "来源单号")
    private String sourceOrderNo;

    @Schema(description = "来源类型 0-收货上架 1-退货上架 2-调拨上架 3-其他")
    private Integer sourceType;

    @Schema(description = "任务状态")
    private Integer status;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "执行人姓名")
    private String assigneeName;

    @Schema(description = "备注")
    private String remark;

    // ── 明细级过滤 ──
    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "商品编码")
    private String productCode;

    @Schema(description = "货位编码（来源或目标）")
    private String locationCode;
}
