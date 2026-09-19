package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券「领用明细」行（对标 13 列）：
 * 客户 / 联系人 / 联系电话 / 优惠券名称 / 类型 / 使用规则 / 面值 /
 * 领用状态 / 单据编号 / 状态 / 领取时间 / 使用时间 / 来源单据
 */
@Data
public class CouponRecordRow {

    private Long id;
    /** 客户（往来单位名称） */
    private String partnerName;
    /** 联系人（主联系人） */
    private String contactName;
    /** 联系电话 */
    private String contactPhone;
    /** 优惠券名称 */
    private String couponName;
    /** 类型 */
    private String couponType;
    /** 使用规则 */
    private String useRule;
    /** 面值 */
    private BigDecimal faceValue;

    /** 领用状态：已领取 / 已使用 / 已过期 / 已作废（由 erp_loyalty_coupon.status 映射） */
    private String receiveStatus;
    /** 单据编号（核销该券的销售单据号） */
    private String billNo;
    /** 状态：券模板状态（正常 / 已作废） */
    private String status;
    /** 领取时间 */
    private LocalDateTime receiveTime;
    /** 使用时间 */
    private LocalDateTime usedTime;
    /** 来源单据 */
    private String sourceBillNo;

    // ── 原始字段（供前端做进一步联动，不参与列展示） ──
    private Long templateId;
    private Long partnerId;
    private String code;
    private String rawStatus;
}
