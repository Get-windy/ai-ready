package cn.aiedge.erp.finance.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产负债表行（左右对照）
 *
 * 一行同时承载资产侧与负债及权益侧的项目，左右按行号对齐；
 * 某侧无项目时该侧字段为空（合计行位于各自侧的最后一行）。
 */
@Data
public class BalanceSheetRowDTO {

    /** 行号（左右对齐依据，从 1 开始） */
    private Integer rowNo;

    // ── 资产侧 ──
    /** 资产项目名称 */
    private String assetItemName;
    /** 资产项目匹配的科目编码（穿透用） */
    private String assetSubjectCode;
    /** 资产项目主科目ID（穿透到明细账用，合计行为空） */
    private Long assetSubjectId;
    /** 资产年初余额 */
    private BigDecimal assetBeginBalance;
    /** 资产期末余额 */
    private BigDecimal assetEndBalance;
    /** 是否资产合计行 */
    private Boolean assetTotalRow = Boolean.FALSE;

    // ── 负债及权益侧 ──
    /** 负债及权益项目名称 */
    private String liabilityItemName;
    /** 负债及权益项目匹配的科目编码（穿透用） */
    private String liabilitySubjectCode;
    /** 负债及权益项目主科目ID（穿透到明细账用，合计行为空） */
    private Long liabilitySubjectId;
    /** 负债及权益年初余额 */
    private BigDecimal liabilityBeginBalance;
    /** 负债及权益期末余额 */
    private BigDecimal liabilityEndBalance;
    /** 是否负债及权益合计类行（负债合计/所有者权益合计/负债和所有者权益总计） */
    private Boolean liabilityTotalRow = Boolean.FALSE;
}
