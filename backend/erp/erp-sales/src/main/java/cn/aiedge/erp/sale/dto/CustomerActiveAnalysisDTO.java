package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户活跃分析行
 * 数据来源: erp_sale_order (deleted=0, 排除已取消) + biz_party_follow (跟进次数)
 */
@Data
public class CustomerActiveAnalysisDTO {

    /** 客户ID (erp_sale_order.customer_id, 对应 biz_party.id) */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 近N天订单数 */
    private Long recentOrderCount;

    /** 近N天订单金额 */
    private BigDecimal recentOrderAmount;

    /** 最近下单时间 */
    private LocalDateTime lastOrderTime;

    /** 历史订单总数 */
    private Long totalOrderCount;

    /** 历史订单总额 */
    private BigDecimal totalOrderAmount;

    /** 跟进次数 (biz_party_follow, 无跟进记录时为0) */
    private Long followCount;

    /** 活跃度分层: 活跃(30天有单)/一般(90天有单)/沉默(90天无单), 阈值可通过参数调整 */
    private String activityLevel;
}
