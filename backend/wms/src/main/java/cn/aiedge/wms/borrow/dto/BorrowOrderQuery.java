package cn.aiedge.wms.borrow.dto;

import lombok.Data;

/**
 * 借进借出单列表查询条件（按单据/按明细共用）
 * 对齐借进借出单页面查询条件。
 */
@Data
public class BorrowOrderQuery {

    private int pageNum = 1;
    private int pageSize = 20;

    /** 方向 1-借进 2-借出 */
    private Integer direction;

    private String dateStart;
    private String dateEnd;
    /** 预计还出时间(起) */
    private String returnDateStart;
    /** 预计还出时间(止) */
    private String returnDateEnd;

    private String orderNo;
    private String partnerName;
    private String partnerCode;
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
