package cn.aiedge.erp.finance.analytics.dto;

import lombok.Data;

/**
 * 财务分析模块（回款统计 / 往来余额表 / 发票统计）报表查询参数。
 *
 * <p>统一一个 DTO：都是「时间区间 + 维度键 + 若干勾选项 + 分页」的账表口径，
 * 拆多个 DTO 只会带来重复样板。未用到的字段留空即可。</p>
 */
@Data
public class AnalyticsQuery {

    /** 视图 Tab（staff=按职员 / dept=按部门 / 其他按各端点定义） */
    private String tab;

    /** 起始日期（YYYY-MM-DD，含） */
    private String startDate;

    /** 结束日期（YYYY-MM-DD，含） */
    private String endDate;

    /** 起始月份（YYYY-MM，含；发票统计按月查询） */
    private String monthStart;

    /** 结束月份（YYYY-MM，含） */
    private String monthEnd;

    /** 年度（提成矩阵） */
    private Integer year;

    /** 日期类型（单据日期/记账日期等，白名单校验） */
    private String dateType;

    /** 部门名称（模糊） */
    private String deptName;

    /** 职员名称（模糊） */
    private String staffName;

    /** 往来单位名称（模糊） */
    private String partnerName;

    /** 单据编号/关键字（模糊） */
    private String keyword;

    /** 仅显示既是客户又是供应商的单位（往来余额表清账场景） */
    private Boolean onlyBoth;

    /** 显示停用 */
    private Boolean showDisabled;

    /** 显示本期金额为 0 的数据 */
    private Boolean showZero;

    /** 包含红冲（查费用：默认不并入红冲单） */
    private Boolean includeReversed;

    /** 不显示为零数据项（提成构成 / 方案汇总提成） */
    private Boolean hideZero;

    /** 进销均无发生的不显示（发票统计） */
    private Boolean hideEmpty;

    /** 查询方式（发票统计：月/季/年；默认按月） */
    private String queryMode;

    /** 配送员名称（模糊） */
    private String riderName;

    /** 配送员角色（全部 / 配送员 / 司机…） */
    private String roleKey;

    /** 提成方案 ID */
    private String planId;

    /** 提成类型 */
    private String typeKey;

    /** 仓库名称（业绩明细） */
    private String warehouseName;

    /** 商品名称/货号（业绩明细） */
    private String productName;

    /** 行维度键（明细钻取用） */
    private String groupKey;

    /** 只显示指定组合键（内部用） */
    private String detailKey;

    /** 页码（从 1 开始） */
    private Integer page;

    /** 每页条数 */
    private Integer size;
}
