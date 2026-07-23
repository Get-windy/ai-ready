package cn.aiedge.erp.sale.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 促销效果分析结果
 * 注: erp_sale_order 没有 promotion_id 关联字段, 订单无法归因到具体促销活动,
 * 因此本结果提供促销活动统计(按状态/类型计数+时间分布)与区间订单优惠概况(全量, 不做活动归因)
 */
@Data
public class PromotionAnalysisDTO {

    /** 按状态计数 [{status, count}] */
    private List<Map<String, Object>> countByStatus;

    /** 按类型计数 [{type, count}] */
    private List<Map<String, Object>> countByType;

    /** 按月时间分布 [{month, count}] */
    private List<Map<String, Object>> monthlyDistribution;

    /** 与查询区间重叠的促销活动列表 */
    private List<ActivityItem> activities;

    /** 区间订单优惠概况 (全量订单口径, 无法归因到具体活动) */
    private DiscountOverview discountOverview;

    /**
     * 促销活动条目 (erp_promotion_activity)
     */
    @Data
    public static class ActivityItem {
        private Long id;
        private String name;
        private String type;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal discountRate;
        private BigDecimal reductionAmount;
        private LocalDateTime createTime;
    }

    /**
     * 区间订单优惠概况
     * 优惠金额 = promo_discount + coupon_amount + direct_discount + discount_amount
     */
    @Data
    public static class DiscountOverview {
        /** 区间内有效订单数 */
        private Long orderCount;
        /** 区间内订单总金额 */
        private BigDecimal totalOrderAmount;
        /** 区间内有优惠的订单数 */
        private Long discountedOrderCount;
        /** 区间内优惠总金额 */
        private BigDecimal totalDiscountAmount;
    }
}
