package cn.aiedge.erp.stock.dto;

import lombok.Data;

/**
 * 拆分单列表查询条件（按单据）
 */
@Data
public class StockSplitQuery {
    private int pageNum = 1;
    private int pageSize = 20;

    private String dateStart;
    private String dateEnd;
    private String splitNo;
    private String keyword;
    private Long inWarehouseId;
    private Long outWarehouseId;
    private String handlerName;
    private String deptName;
    private String creatorName;
    private String bookkeeperName;
    private Integer status;
    private String remark;
    private String summary;
    private Boolean showRed;
}
