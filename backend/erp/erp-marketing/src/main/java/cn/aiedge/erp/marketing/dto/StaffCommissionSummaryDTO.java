package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 员工提成汇总行
 * 数据来源: erp_commission_record 按 referrer_id (推荐人=员工/业务员) 聚合
 * 姓名来源: sys_user (real_name > nickname > username), 关联不上时为 null
 */
@Data
public class StaffCommissionSummaryDTO {

    /** 推荐人ID (erp_commission_record.referrer_id) */
    private Long referrerId;

    /** 员工姓名 (sys_user.real_name/nickname/username, 关联不上为 null) */
    private String staffName;

    /** 提成记录数 */
    private Long recordCount;

    /** 成单数 (去重订单) */
    private Long orderCount;

    /** 订单金额合计 */
    private BigDecimal totalOrderAmount;

    /** 提成金额合计 */
    private BigDecimal totalCommissionAmount;

    /** 已结算提成金额 (status=PAID) */
    private BigDecimal settledCommissionAmount;

    /** 已结算记录数 (status=PAID) */
    private Long settledCount;

    /** 未结算提成金额 (status=DRAFT/CONFIRMED) */
    private BigDecimal unsettledCommissionAmount;

    /** 未结算记录数 (status=DRAFT/CONFIRMED) */
    private Long unsettledCount;
}
