package cn.aiedge.erp.finance.otherincome.dto;

import cn.aiedge.erp.finance.otherincome.entity.OtherIncomeDocItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 其他收入明细分页 VO（按明细 tab）
 *
 * 在 OtherIncomeDocItem 基础上补充主表单据维度字段，
 * 前端"按明细"tab 的 18 列可直接 flat 访问。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OtherIncomeItemDetailVO extends OtherIncomeDocItem {

    /** 单据日期（主表） */
    private LocalDate incomeDate;

    /** 单据编号（主表） */
    private String docNo;

    /** 单据状态（主表） */
    private Integer docStatus;

    /** 结算状态（主表） */
    private Integer settleStatus;

    /** 往来单位（主表） */
    private String partnerName;

    /** 往来编号（主表） */
    private String partnerCode;

    /** 经手人（主表） */
    private String handlerName;

    /** 部门（主表） */
    private String departmentName;

    /** 制单人（主表） */
    private String creatorName;

    /** 记账人（主表） */
    private String bookkeeperName;

    /** 摘要（主表） */
    private String summary;

    /** 单据备注（主表） */
    private String docRemark;

    /** 制单时间（主表） */
    private LocalDateTime docCreateTime;

    /** 记账时间（主表） */
    private LocalDateTime bookkeepingTime;

    /** 打印次数（主表） */
    private Integer printCount;

    public BigDecimal safeAmount(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
