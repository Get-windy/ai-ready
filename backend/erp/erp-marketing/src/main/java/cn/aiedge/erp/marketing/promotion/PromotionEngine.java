package cn.aiedge.erp.marketing.promotion;

/**
 * 促销引擎（服务端优惠计算的**单一真源**）。
 *
 * <p>口径（对标 SAP Commerce Promotion Engine / Odoo Discount &amp; Loyalty / Dynamicweb）：
 * <ol>
 *   <li><b>命中判定</b>：活动生效（时间 + 状态 + 使用范围 + 促销客户）且范围匹配（商品 / 整单门槛 / 特价商品）</li>
 *   <li><b>优先级</b>：按 {@code priority} 降序计算，同优先级按 id 升序兜底（避免 SAP 指出的"同优先级随机选择"）</li>
 *   <li><b>叠加 / 互斥</b>：{@code stack_policy=EXCLUSIVE} 的活动命中后，不再计算更低优先级活动</li>
 *   <li><b>条目消耗</b>：同一商品行只被**一个**行级优惠（商品促销 / 特价）消耗，高优先级先占，
 *       杜绝同一商品被重复打折（SAP Order Entry Consumption）</li>
 *   <li><b>优惠分摊</b>：行级优惠摊到命中行；整单级优惠按各**未被打折行**金额占比摊到行上，
 *       产出 {@link PromotionAllocation} 明细落 {@code erp_sale_order_promo_detail}</li>
 *   <li><b>封顶</b>：{@code max_discount_amount} 限制单条活动在本单的优惠上限</li>
 *   <li><b>券</b>：校验券状态 / 有效期 / 门槛后计入 {@code couponDiscount}；核销由
 *       {@link CouponRedemptionService} 在订单事务内完成（本引擎只读校验 + 返回待核销券列表）</li>
 * </ol>
 *
 * <p><b>边界（如实标注）</b>：满赠类活动命中后只登记赠品分配（{@code giftProductId/quantity}），
 * **不自动加赠品行**——加行涉及库存与单据一致性，由开单侧确认后处理。</p>
 */
public interface PromotionEngine {

    /** 计算一次结算的全部优惠（纯计算，无副作用） */
    PromotionResult evaluate(PromotionRequest request);

    /** 试算入口（供开单页联调/预览；与 evaluate 同口径，仅语义命名区分） */
    default PromotionResult preview(PromotionRequest request) {
        return evaluate(request);
    }

    /**
     * 登记活动使用次数（命中活动落单后调用，与订单同事务）。
     * 与 {@link #evaluate} 分离，保证「试算不累加次数、只有真正落单才累加」。
     */
    void recordUsage(java.util.List<Long> promotionIds);
}
