package cn.aiedge.wms.pick.dto;

import lombok.Data;

/**
 * 拣货单列表查询条件（按单据/按明细共用）
 * 对齐拣货单页面查询条件。
 */
@Data
public class PickTaskQuery {

    private int pageNum = 1;
    private int pageSize = 20;

    private String dateStart;
    private String dateEnd;

    private String taskNo;
    private String keyword;
    private String sourceOrderNo;
    private String customerName;
    private Long warehouseId;
    private String warehouseName;
    private Integer sourceType;
    private Integer status;
    private Integer priority;
    private String remark;

    // ── 按明细查询（page-detail） ──
    private String productName;
    private String locationCode;
    private String itemRemark;
}
