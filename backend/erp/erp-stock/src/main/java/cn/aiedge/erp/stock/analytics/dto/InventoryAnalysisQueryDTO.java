package cn.aiedge.erp.stock.analytics.dto;

import lombok.Data;

/**
 * 进销存分析（分析 → 仓配分析 → 进销存分析）聚合查询参数
 */
@Data
public class InventoryAnalysisQueryDTO {

    /** 页码（从 1 起） */
    private Integer page = 1;

    /** 每页行数（对标实测 20 行/页） */
    private Integer size = 20;

    /**
     * 视图 Tab（白名单枚举）：
     * <ul>
     *   <li>product —— 按商品（进销存主账，38 列口径）</li>
     *   <li>transferWarehouse —— 仓库调拨分析（出库仓 → 入库仓）</li>
     *   <li>transferProduct —— 商品调拨分析（商品维度调拨汇总）</li>
     * </ul>
     */
    private String tab;

    /** 期间开始日期 yyyy-MM-dd（含） */
    private String startDate;

    /** 期间结束日期 yyyy-MM-dd（含） */
    private String endDate;

    /** 出库仓库名称模糊（调拨两视图的「出库仓库」） */
    private String outWarehouseName;

    /** 入库仓库名称模糊（调拨两视图的「入库仓库」） */
    private String inWarehouseName;

    /** 商品关键字（名称/货号/条码） */
    private String keyword;

    /** 品牌模糊 */
    private String brand;

    /** 显示红冲（勾选后已取消/已红冲单据参与统计，默认不勾选） */
    private Boolean showReversed;
}
