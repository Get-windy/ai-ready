package cn.aiedge.wms.warehouse.dto;

import lombok.Data;

/**
 * 货位列表查询条件（对标 ql361：仓库规划 → 2.货位）
 */
@Data
public class LocationQuery {

    private int pageNum = 1;
    private int pageSize = 20;

    /** 仓库（所属仓库名称 / 编号 模糊匹配） */
    private String warehouseKeyword;

    /** 仓库ID（精确） */
    private Long warehouseId;

    /** 货位编号（模糊匹配） */
    private String locationCode;

    /** 显示停用（对标复选框，默认 false） */
    private Boolean showDisabled = Boolean.FALSE;
}
