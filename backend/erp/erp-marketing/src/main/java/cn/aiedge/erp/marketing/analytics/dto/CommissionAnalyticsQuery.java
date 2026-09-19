package cn.aiedge.erp.marketing.analytics.dto;

import lombok.Data;

/**
 * 业绩提成中心六视图查询参数。
 *
 * <p>erp-marketing 不依赖 erp-finance，故本模块自带查询 DTO（不跨模块复用 AnalyticsQuery）。</p>
 */
@Data
public class CommissionAnalyticsQuery {

    /** 视图 Tab（riders / monthly / composition / planSummary / overview / detail） */
    private String tab;

    /** 年度（矩阵类视图口径） */
    private Integer year;

    /** 起始日期（业绩明细日期区间口径） */
    private String startDate;

    /** 结束日期 */
    private String endDate;

    /** 配送员（模糊） */
    private String riderName;

    /** 配送员角色（全部 / 企业员工 / 众包兼职 / 外部平台配送员 / 社会车辆司机） */
    private String roleKey;

    /** 提成方案名称（模糊） */
    private String planName;

    /** 提成类型 */
    private String typeKey;

    /** 不显示为零数据项 */
    private Boolean hideZero;

    /** 部门（模糊） */
    private String deptName;

    /** 仓库（模糊） */
    private String warehouseName;

    /** 商品名称/货号（模糊） */
    private String productName;

    /** 页码 */
    private Integer page;

    /** 每页条数 */
    private Integer size;
}
