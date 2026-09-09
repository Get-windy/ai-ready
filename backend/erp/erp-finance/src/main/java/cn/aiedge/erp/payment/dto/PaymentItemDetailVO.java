package cn.aiedge.erp.payment.dto;

import cn.aiedge.erp.payment.entity.PaymentItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款明细分页 VO（按明细 tab）
 *
 * 在 PaymentItem 基础上补充付款单主表单据维度字段，
 * 前端"付款明细"tab 的 22 列可直接 flat 访问。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentItemDetailVO extends PaymentItem {

    /** 单据日期（主表） */
    private LocalDate paymentDate;

    /** 单据编号（主表） */
    private String paymentNo;

    /** 单据状态（主表） */
    private Integer paymentStatus;

    /** 结算单位（主表） */
    private String supplierName;

    /** 结算单位编号（主表） */
    private String supplierCode;

    /** 经手人（主表） */
    private String handlerName;

    /** 部门（主表） */
    private String deptName;

    /** 记账人（主表） */
    private String bookkeeperName;

    /** 制单人（主表） */
    private String creatorName;

    /** 审核人（主表） */
    private String auditorName;

    /** 单据备注（主表） */
    private String docRemark;

    /** 记账时间（主表） */
    private String bookkeepingTime;

    /** 打印次数（主表） */
    private Integer printCount;

    public BigDecimal safeAmount(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
