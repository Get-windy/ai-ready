package cn.aiedge.erp.marketing.promotion;

/**
 * 优惠券核销（券生命周期闭环：发放 → 领取 → **核销** → 退货回滚 → 过期）。
 *
 * <p>核销必须与销售订单**同一事务**：核销成功才允许订单落库，订单取消/退货需回滚券状态。</p>
 */
public interface CouponRedemptionService {

    /**
     * 核销券（幂等：已核销到同一订单视为成功）
     *
     * @param couponId 券实例 id
     * @param orderId  核销该券的销售订单 id
     * @param orderNo  销售订单号（写来源单据用）
     */
    void redeem(Long couponId, Long orderId, String orderNo);

    /**
     * 回滚核销（订单取消 / 退货时把券退回「已领取未使用」）
     *
     * @return 实际回滚的券张数
     */
    int rollbackByOrder(Long orderId);
}
