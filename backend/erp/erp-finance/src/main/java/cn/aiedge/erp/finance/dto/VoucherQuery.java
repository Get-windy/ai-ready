package cn.aiedge.erp.finance.dto;

import lombok.Data;

/**
 * 凭证分页查询条件
 * 对应列表页页面配置「查询条件」Tab 全量字段；核算单位/部门/职员、表头自定义无真实表列，
 * 不作为查询条件（前端列配置保留，查询忽略）。
 */
@Data
public class VoucherQuery {

    /** 凭证日期起 */
    private String startDate;

    /** 凭证日期止 */
    private String endDate;

    /** 单据编号（模糊） */
    private String keyword;

    /** 来源单据编号 */
    private String sourceNo;

    /** 单据类型 (manual/system) */
    private String voucherType;

    /** 摘要（凭证头） */
    private String summary;

    /** 单据状态 (draft/audited/posted/reversed) */
    private String status;

    /** 科目ID（联查分录） */
    private Long subjectId;

    /** 经手人 */
    private String handlerName;

    /** 部门 */
    private String deptName;

    /** 制单人 */
    private String prepBy;

    /** 记账人/审核人 */
    private String verifyBy;

    /** 显示红冲 (true=包含红字/冲销凭证) */
    private Boolean showRed;
}
