package cn.aiedge.docquery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 综合单据查询参数（分析 &gt; 综合单据）
 *
 * <p>对标 ql361「待审批单据 / 业务草稿 / 经营历程」三页的查询区，逐项对应：</p>
 * <ul>
 *   <li>待审批单据：单据编号 / 单据类型 / 往来单位 / 经手人 / 部门 / 单据备注 / 账期(比较符+天数) / 仓库 / 所属区域</li>
 *   <li>业务草稿：单据编号 / 单据类型 / 往来单位 / 仓库 / 经手人 / 部门 / 制单人 / 单据备注 / 来源订单</li>
 *   <li>经营历程：单据类型 / 单据编号 / 往来单位 / 仓库 / 经手人 / 部门 / 显示红冲</li>
 * </ul>
 */
@Data
@Schema(description = "综合单据查询参数")
public class DocQueryParams {

    /** 单据类型代码，如 SALE_ORDER，空为全部 */
    private String docType;

    /** 单据编号模糊 */
    private String docNo;

    /** 往来单位模糊 */
    private String partnerName;

    /** 业务日期起 yyyy-MM-dd（含） */
    private String startDate;

    /** 业务日期止 yyyy-MM-dd（含） */
    private String endDate;

    /** 仓库模糊 */
    private String warehouseName;

    /** 所属区域模糊 */
    private String region;

    /** 经手人模糊 */
    private String handlerName;

    /** 部门模糊 */
    private String departmentName;

    /** 制单人模糊 */
    private String creatorName;

    /** 单据备注模糊 */
    private String remark;

    /** 来源订单号模糊 */
    private String sourceOrderNo;

    /**
     * 账期比较符：ge（≥，默认）/ le（≤）/ eq（=）/ gt（>）/ lt（&lt;）
     *
     * <p>账期天数 = 单据上「收/付款截止日」−「单据日期」；无该字段的单据类型为 NULL，不参与比较。</p>
     */
    private String accountPeriodOp;

    /** 账期天数 */
    private Integer accountPeriodDays;

    /** 显示红冲：false（默认）= 排除已取消单据；true = 全部显示 */
    private Boolean includeReversed;

    /** 排序字段：bizDate（默认）/ amount */
    private String sortField;

    /** 排序方向：desc（默认）/ asc */
    private String sortOrder;

    /** 页码，从 1 开始 */
    private int page = 1;

    /** 每页条数，最大 100 */
    private int size = 20;
}
