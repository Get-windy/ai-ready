package cn.aiedge.erp.stock.dto;

import lombok.Data;

/**
 * 报损单列表查询条件（按单据/按明细共用）
 */
@Data
public class StockDamageQuery {
    private int pageNum = 1;
    private int pageSize = 20;

    private String dateStart;
    private String dateEnd;
    private String damageNo;
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
    private Integer damageCause;
    private Boolean showRed;
}
