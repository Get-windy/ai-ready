package cn.aiedge.wms.move.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 移库任务多条件分页查询.
 * <p>对齐报损单 StockDamageQuery：pageNum/pageSize 前端分页 + 按单据/按明细两组过滤。</p>
 */
@Data
@Schema(description = "移库任务分页查询")
public class MoveTaskQuery {

    @Schema(description = "页码，从1开始")
    private long pageNum = 1;

    @Schema(description = "每页条数")
    private long pageSize = 20;

    @Schema(description = "单号（模糊）")
    private String taskNo;

    @Schema(description = "关键词（单号模糊，与 taskNo 同义）")
    private String keyword;

    @Schema(description = "仓库ID")
    private Long warehouseId;

    @Schema(description = "状态 0待移库 1移库中 2已完成 3已取消")
    private Integer status;

    @Schema(description = "移库类型 1库内 2补货 3整理")
    private Integer moveType;

    @Schema(description = "来源类型 0手动 1盘差 2补货 3其他")
    private Integer sourceType;

    @Schema(description = "来源单号（模糊）")
    private String sourceNo;

    @Schema(description = "源货位编码")
    private String fromLocationCode;

    @Schema(description = "目标货位编码")
    private String toLocationCode;

    @Schema(description = "执行人")
    private String assigneeName;

    @Schema(description = "制单开始日期 yyyy-MM-dd")
    private LocalDate dateStart;

    @Schema(description = "制单结束日期 yyyy-MM-dd")
    private LocalDate dateEnd;

    // ── 按明细 Tab 过滤 ──
    @Schema(description = "商品名称（按明细，模糊）")
    private String productName;

    @Schema(description = "明细备注（按明细，模糊）")
    private String itemRemark;
}
