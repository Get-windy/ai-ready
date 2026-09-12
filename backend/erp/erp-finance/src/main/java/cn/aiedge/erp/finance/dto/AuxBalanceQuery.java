package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.util.List;

/**
 * 辅助核算余额表查询条件
 *
 * 口径（P0 红线）：辅助核算余额只读取自凭证分录（finance_voucher_item ⨝ finance_voucher），
 * 仅取已记账凭证（posted），严禁绕过凭证直改核算项余额。
 */
@Data
public class AuxBalanceQuery {

    /** 会计月(起)，格式 yyyy-MM；为空时默认取会计月(止) */
    private String startMonth;

    /** 会计月(止)，格式 yyyy-MM；为空时默认取当前月 */
    private String endMonth;

    /** 科目ID（含全部下级科目） */
    private Long subjectId;

    /** 科目编码（精确匹配，优先于 subjectId） */
    private String subjectCode;

    /** 核算项类型：PARTNER(往来单位) / CUSTOMER(客户) / SUPPLIER(供应商) / EMPLOYEE(职员) / DEPT(部门) */
    private String auxType;

    /** 核算项编码/名称模糊查询 */
    private String keyword;

    /** 无本期发生额不显示 */
    private Boolean hideNoPeriodAmount;

    /** 余额为0不显示 */
    private Boolean hideZeroBalance;

    private Integer pageNum;

    private Integer pageSize;

    // ═══ 以下为服务端解析出的派生条件（前端无需传入） ═══

    /** 科目范围（本级 + 全部下级科目编码） */
    private List<String> subjectCodes;

    /** 会计月(起)期间键 = 会计年 * 100 + 会计月 */
    private Integer startPeriodKey;

    /** 会计月(止)期间键 = 会计年 * 100 + 会计月 */
    private Integer endPeriodKey;

    /** 止月所属会计年（本年累计口径） */
    private Integer endFiscalYear;

    /** 止月会计月（本年累计口径） */
    private Integer endFiscalMonth;
}
