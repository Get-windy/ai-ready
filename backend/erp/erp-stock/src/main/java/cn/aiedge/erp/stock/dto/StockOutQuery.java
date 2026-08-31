package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 其他出库单列表查询条件（按单据/按明细共用）
 */
@Data
public class StockOutQuery {
    private int pageNum = 1;
    private int pageSize = 20;

    private String dateStart;
    private String dateEnd;
    private String stockOutNo;
    private String keyword;
    private String partnerName;
    private String customerName;
    private String handlerName;
    private String deptName;
    private String creatorName;
    private String bookkeeperName;
    private Long warehouseId;
    private String warehouseName;
    private Integer status;
    private String remark;
    private String itemRemark;
    private String productName;
    private Boolean showRed;
}
