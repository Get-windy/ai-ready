package cn.aiedge.erp.stock.dto;

import lombok.Data;

/**
 * 组装单列表查询条件（按单据）
 */
@Data
public class StockAssembleQuery {
    private int pageNum = 1;
    private int pageSize = 20;

    private String dateStart;
    private String dateEnd;
    private String assembleNo;
    private String keyword;
    private String produceUnit;
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
