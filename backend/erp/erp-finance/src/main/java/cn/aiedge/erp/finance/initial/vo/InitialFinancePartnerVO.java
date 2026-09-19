package cn.aiedge.erp.finance.initial.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务期初（按往来单位）列表行。
 *
 * <p>⚠️ NON_NULL 口径同 {@link InitialFinanceSubjectVO}：应付期初不含 {@code defaultHandler}
 * 与应收/预收金额列，应收期初不含应付/预付金额列 —— 字段集与对标 Tab 列集严格一致。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitialFinancePartnerVO {

    /** 主键 */
    private Long id;

    /** 期初类型 PAYABLE / RECEIVABLE */
    private String initialType;

    /** 期初年度 */
    private Integer periodYear;

    /** 往来单位 ID */
    private Long partnerId;

    /** 供应商编号 / 客户编号 */
    private String partnerCode;

    /** 供应商名称 / 客户名称 */
    private String partnerName;

    /** 默认经手人（仅应收期初） */
    private String defaultHandler;

    /** 应付金额（仅应付期初） */
    private BigDecimal payableAmount;

    /** 预付金额（仅应付期初） */
    private BigDecimal prepayAmount;

    /** 应收金额（仅应收期初） */
    private BigDecimal receivableAmount;

    /** 预收金额（仅应收期初） */
    private BigDecimal advanceAmount;

    /** 创建时间（已格式化） */
    private String createTime;
}
