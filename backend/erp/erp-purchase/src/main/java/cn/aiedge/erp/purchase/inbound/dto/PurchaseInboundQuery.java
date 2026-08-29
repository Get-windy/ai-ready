package cn.aiedge.erp.purchase.inbound.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购入库单分页查询条件（对齐列表页 21 个搜索字段）。
 * 分页 + 全部可搜索字段，供列表页按需传参。
 */
@Data
public class PurchaseInboundQuery {

    // ── 分页 ──
    private int pageNum = 1;
    private int pageSize = 10;

    // ── 兼容旧参数 ──
    private String keyword;
    private Long supplierId;
    private Long orderId;
    private Long warehouseId;
    private Integer status;

    // ── 列表页搜索字段 ──
    private String inboundNo;
    private String supplierName;
    private String orderNo;
    private String purchaserName;
    private String departmentName;
    private String createByName;
    private String posterName;
    private String approvedByName;
    private String warehouseName;
    private String remark;
    private Integer settleStatus;
    private BigDecimal extNum1;
    private BigDecimal extNum2;
    private String extText1;
    private String extText2;
    private String extText3;
    private Integer printCount;

    // ── 日期范围 ──
    private LocalDate startDate;
    private LocalDate endDate;
}
