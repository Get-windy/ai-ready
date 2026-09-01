package cn.aiedge.erp.stock.dto;

import lombok.Data;

/**
 * 成本调价单列表查询条件（按单据/按明细共用）
 */
@Data
public class StockCostAdjustQuery {

    private int pageNum = 1;

    private int pageSize = 20;

    private String dateStart;

    private String dateEnd;

    private String adjustNo;

    private String keyword;

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

    private String reasonType;

    private Boolean showRed;
}
