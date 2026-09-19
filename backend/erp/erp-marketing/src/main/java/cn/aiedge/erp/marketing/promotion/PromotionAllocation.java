package cn.aiedge.erp.marketing.promotion;

import lombok.Data;

import java.math.BigDecimal;

/** 促销引擎输出：一条优惠在一行明细上的分摊结果（落 erp_sale_order_promo_detail） */
@Data
public class PromotionAllocation {

    /** 促销活动 id（券优惠时为空） */
    private Long promotionId;
    private String promotionName;
    /** 促销模式：打折 / 满减 / 特价 / 满赠 / 优惠券 */
    private String promoMode;
    /** ORDER 整单级 / ITEM 商品行级 */
    private String scopeType;
    /** 分摊到哪一行（整单级优惠按行金额占比摊到每一行） */
    private Integer lineNo;
    private Long productId;
    /** 该行分摊到的优惠额 */
    private BigDecimal discountAmount;
    /** 券优惠专用 */
    private Long couponId;
    private String couponCode;
    /** 满赠类：赠品信息（不计金额优惠） */
    private Long giftProductId;
    private BigDecimal giftQuantity;
    private String remark;
}
