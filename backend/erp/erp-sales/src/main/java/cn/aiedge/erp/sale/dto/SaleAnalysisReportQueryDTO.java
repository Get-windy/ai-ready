package cn.aiedge.erp.sale.dto;

import lombok.Data;

/**
 * 销售分析组（销售业绩 / 销售分析 / 销售履约分析 / 销售欠款分析）聚合查询参数
 *
 * <p>四个页面共用同一套查询条件；各页按自身对标查询区取用其中一部分。</p>
 */
@Data
public class SaleAnalysisReportQueryDTO {

    /** 页码（从 1 起） */
    private Integer page = 1;

    /** 每页行数 */
    private Integer size = 20;

    /**
     * 视图 Tab 标识（逐页不同，均为白名单枚举，服务端按表映射维度）：
     * <ul>
     *   <li>销售业绩：time / staff</li>
     *   <li>销售分析：time / product / brand / customer / region / warehouse / staff / source</li>
     *   <li>销售履约分析：doc / customer</li>
     *   <li>销售欠款分析：staff / customer / region</li>
     * </ul>
     */
    private String tab;

    /** 起始日期 yyyy-MM-dd（含） */
    private String startDate;

    /** 结束日期 yyyy-MM-dd（含） */
    private String endDate;

    /** 客户 ID（erp_sale_* / biz_party 主键，字符串透传雪花 ID） */
    private String customerId;

    /** 客户名称模糊 */
    private String customerName;

    /** 经手人 / 职员 ID（仅名称过滤可用，保留字段与对标查询项对齐） */
    private String salesmanId;

    /** 经手人 / 职员名称模糊 */
    private String salesmanName;

    /** 部门 ID */
    private String deptId;

    /** 部门名称模糊 */
    private String deptName;

    /** 仓库 ID */
    private String warehouseId;

    /** 仓库名称模糊 */
    private String warehouseName;

    /** 区域模糊 */
    private String region;

    /** 商品关键字（名称/货号/条码） */
    private String keyword;

    /** 品牌模糊 */
    private String brand;

    /** 来源 / 产生方式模糊 */
    private String source;

    /** 销售类型（对标「销售类型」下拉，透传到 sale_type） */
    private Integer saleType;

    /** 时间粒度：day（默认）/ week / month，仅「按时间」维度生效 */
    private String granularity;

    /** 不显示停用职员（按 sys_user.status 剔除停用人员） */
    private Boolean hideDisabledStaff;

    /** 仅显示销售数量大于 0 的行（对标「显示销售数量大于0」勾选） */
    private Boolean onlyPositiveQty;

    /** 单据状态多选（销售履约分析用，逗号分隔的状态码） */
    private String docStatus;

    /** 客户分类ID多选（销售履约分析左侧客户分类树；逗号分隔，含所选节点及其全部下级） */
    private String customerCategoryIds;
}
