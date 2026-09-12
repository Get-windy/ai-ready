package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.util.List;

/**
 * 明细账查询条件
 *
 * 对应页面「明细账」查询区 12 字段：日期 / 日期类型 / 科目 / 核算单位 / 核算部门 /
 * 核算职员 / 经手人 / 部门 / 对账标记 / 摘要 / 单据备注 / 显示红冲。
 */
@Data
public class LedgerDetailQuery {

    /** 页码（从 1 开始） */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 20;

    /** 日期起（yyyy-MM-dd） */
    private String dateStart;

    /** 日期止（yyyy-MM-dd） */
    private String dateEnd;

    /** 日期类型：voucher-单据日期(默认) / post-记账日期 */
    private String dateType;

    /** 科目ID（科目树选中单科目时） */
    private Long subjectId;

    /** 科目编码集合（含下级科目，由科目树选中科目展开得到） */
    private List<String> subjectCodes;

    /** 科目类型 1-资产 2-负债 3-权益 4-成本 5-损益（科目树选中「XX类」时） */
    private Integer subjectType;

    /** 核算单位 */
    private String settleUnit;

    /** 核算部门 */
    private String settleDept;

    /** 核算职员 */
    private String settleStaff;

    /** 经手人 */
    private String handlerName;

    /** 部门 */
    private String deptName;

    /** 对账标记 0-未对账 1-已对账（null=全部） */
    private Integer reconcileFlag;

    /** 摘要 */
    private String summary;

    /** 单据备注 */
    private String remark;

    /** 显示红冲（true=包含已冲销凭证） */
    private Boolean showRed = false;
}
