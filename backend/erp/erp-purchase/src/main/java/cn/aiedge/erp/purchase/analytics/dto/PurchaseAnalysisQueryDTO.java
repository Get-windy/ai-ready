package cn.aiedge.erp.purchase.analytics.dto;

import lombok.Data;

/**
 * 采购分析（分析 → 采销分析 → 采购分析）聚合查询参数
 *
 * <p>对标的「8 段时间快捷段」由前端换算为 startDate/endDate 传入，服务端只认区间。</p>
 */
@Data
public class PurchaseAnalysisQueryDTO {

    /** 页码（从 1 起） */
    private Integer page = 1;

    /** 每页行数 */
    private Integer size = 20;

    /**
     * 视图 Tab（白名单枚举）：
     * <ul>
     *   <li>time —— 按时间（按天/周/月粒度，粒度切换走 granularity）</li>
     *   <li>product —— 按商品</li>
     *   <li>supplier —— 按供应商</li>
     * </ul>
     */
    private String tab;

    /** 起始日期 yyyy-MM-dd（含） */
    private String startDate;

    /** 结束日期 yyyy-MM-dd（含） */
    private String endDate;

    /** 供应商名称模糊 */
    private String supplierName;

    /** 商品关键字（名称/货号/条码） */
    private String keyword;

    /** 经手人名称模糊（采购订单/入库单/退货单的 purchaser_name） */
    private String handlerName;

    /** 仓库名称模糊 */
    private String warehouseName;

    /** 品牌模糊 */
    private String brand;

    /** 部门名称模糊 */
    private String deptName;

    /** 时间粒度：day（默认）/ week / month，仅「按时间」维度生效 */
    private String granularity;
}
