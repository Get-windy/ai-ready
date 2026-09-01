package cn.aiedge.erp.stock.dto;

import lombok.Data;

/**
 * 盘点单列表查询条件（按单据/按明细/未盘商品共用）
 */
@Data
public class StockTakeQuery {
    private int pageNum = 1;
    private int pageSize = 20;

    private String dateStart;
    private String dateEnd;
    private String stockTakeNo;
    private String keyword;
    private Integer checkMethod;
    private Integer checkType;
    private Long warehouseId;
    private String warehouseName;
    private String regionName;
    private Long handlerId;
    private String handlerName;
    private Long deptId;
    private String deptName;
    private String creatorName;
    private String bookkeeperName;
    private String linkedBillNo;
    private Integer status;
    private String remark;
    private String itemRemark;
    private String productName;
    private String barcode;
    /** 未盘商品查询：按仓库筛选 */
    private Long checkWarehouseId;
    /** 未盘商品查询：货位 */
    private String location;
    /** 未盘商品查询：商品编码/名称 */
    private String productKeyword;
    /** 未盘商品查询：筛选条件（全部/已盘/未盘） */
    private String filterCondition;
    /** 未盘商品查询：显示状态（全部/有货/无货） */
    private String showStatus;
    /** 未盘商品查询：是否包含已处理商品 */
    private Boolean includeHandled;
    /** 未盘商品查询：排除的盘点单ID */
    private Long excludeStockTakeId;
    /** 未盘商品查询：可用库存下限 */
    private java.math.BigDecimal availableStockMin;
    /** 未盘商品查询：账面库存下限 */
    private java.math.BigDecimal bookStockMin;
}
