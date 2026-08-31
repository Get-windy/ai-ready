package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 调拨单分页查询条件（对齐列表页「按单据/按明细」搜索字段）。
 */
@Data
public class StockTransferQuery {

    // ── 分页 ──
    private int pageNum = 1;
    private int pageSize = 10;

    // ── 兼容旧参数 ──
    private String keyword;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private Integer status;

    // ── 列表页搜索字段（按单据tab） ──
    private String transferNo;
    private String sourceBillNo;
    private String applicantName;
    private String departmentName;
    private String createByName;
    private String posterName;
    private String fromWarehouseName;
    private String toWarehouseName;
    private Integer transferType;
    private String remark;

    // ── 按明细tab附加搜索字段 ──
    private String productName;
    private String itemRemark;

    // ── 显示红冲 ──
    private Boolean showRed;

    // ── 自定义字段（表体数字） ──
    private BigDecimal extNum1;
    private BigDecimal extNum2;

    // ── 日期范围 ──
    private LocalDate startDate;
    private LocalDate endDate;
}
