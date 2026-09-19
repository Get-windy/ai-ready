package cn.aiedge.erp.finance.initial.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务期初（按科目）新增/编辑入参。
 *
 * <p>字段与 ql361「按科目」Tab 一一对应：科目编号 / 科目名称（快照）+ 期初金额，
 * 资产负债 Tab 额外带借贷方向。{@code id} 为空 = 新增，非空 = 更新（更新时
 * **科目不可改**，见 InitialFinanceService#updateSubject）。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class InitialFinanceSubjectDTO {

    /** 主键（雪花 ID 字符串；空 = 新增） */
    private Long id;

    /** 期初类型：BANK_CASH / FIXED_ASSET / BALANCE_SHEET */
    private String initialType;

    /** 期初年度（2000–2099） */
    private Integer periodYear;

    /** 会计科目 ID */
    private Long subjectId;

    /** 科目编号（快照；留空时后端按 subjectId 从科目档案补全） */
    private String subjectCode;

    /** 科目名称（快照；留空时后端按 subjectId 从科目档案补全） */
    private String subjectName;

    /** 借贷方向 DEBIT/CREDIT（仅资产负债期初有效，其余类型后端强制置空） */
    private String direction;

    /** 期初金额（允许负数，与对标「银行存款期初 -390,048.78」一致） */
    private BigDecimal openingAmount;
}
