package cn.aiedge.erp.sale.preorder.analytics.dto;

import lombok.Data;

/**
 * 预订货查询（分析 → 采销分析 → 销售分析 → 预订货查询）汇总聚合查询参数
 */
@Data
public class PreOrderAnalysisQueryDTO {

    /** 页码（从 1 起） */
    private Integer page = 1;

    /** 每页行数 */
    private Integer size = 20;

    /**
     * 视图 Tab（白名单枚举）：
     * <ul>
     *   <li>product —— 商品预订货分析（28 列口径）</li>
     *   <li>customer —— 客户预订货分析（23 列口径）</li>
     * </ul>
     */
    private String tab;

    /** 起始日期 yyyy-MM-dd（含，按预订货单 order_date） */
    private String startDate;

    /** 结束日期 yyyy-MM-dd（含） */
    private String endDate;

    /** 客户名称模糊 */
    private String customerName;

    /** 商品关键字（名称/货号/条码） */
    private String keyword;

    /** 经手人名称模糊 */
    private String handlerName;
}
