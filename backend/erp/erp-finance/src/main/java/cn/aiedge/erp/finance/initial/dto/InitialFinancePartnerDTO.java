package cn.aiedge.erp.finance.initial.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务期初（按往来单位）新增/编辑入参。
 *
 * <p>应付 Tab 只用 {@code payableAmount}/{@code prepayAmount}；
 * 应收 Tab 只用 {@code defaultHandler}/{@code receivableAmount}/{@code advanceAmount}。
 * 与期初类型无关的金额列由服务层**强制置空**，避免出现「应收行带应付金额」的脏数据。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
public class InitialFinancePartnerDTO {

    /** 主键（雪花 ID 字符串；空 = 新增） */
    private Long id;

    /** 期初类型：PAYABLE 应付 / RECEIVABLE 应收 */
    private String initialType;

    /** 期初年度（2000–2099） */
    private Integer periodYear;

    /** 往来单位 ID（往来单位档案 biz_party.id） */
    private Long partnerId;

    /** 供应商编号 / 客户编号（快照） */
    private String partnerCode;

    /** 供应商名称 / 客户名称（快照） */
    private String partnerName;

    /** 默认经手人（仅应收期初有效，应付 Tab 传了也会被置空） */
    private String defaultHandler;

    /** 应付金额（应付 Tab） */
    private BigDecimal payableAmount;

    /** 预付金额（应付 Tab） */
    private BigDecimal prepayAmount;

    /** 应收金额（应收 Tab） */
    private BigDecimal receivableAmount;

    /** 预收金额（应收 Tab） */
    private BigDecimal advanceAmount;
}
