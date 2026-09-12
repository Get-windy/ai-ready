package cn.aiedge.erp.stock.dto;

import lombok.Data;

/**
 * 仓库规划列表查询条件（对标 ql361：资料 → 仓库管理 → 仓库规划）
 */
@Data
public class WarehouseQuery {

    private int pageNum = 1;
    private int pageSize = 20;

    /** 筛选条件：编号/名称/联系人/联系电话/地址/助记码 模糊匹配 */
    private String keyword;

    /** 所属分类ID（左树选中节点） */
    private Long categoryId;

    /** 状态 1-启用 0-停用 */
    private Integer status;

    /** 显示停用（对标复选框，默认 false） */
    private Boolean showDisabled = Boolean.FALSE;

    /** 显示层次结构（对标复选框，默认 true） */
    private Boolean showHierarchy = Boolean.TRUE;
}
