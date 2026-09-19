package cn.aiedge.erp.marketing.promotion;

import cn.aiedge.erp.marketing.entity.CouponTemplate;
import cn.aiedge.erp.marketing.entity.LoyaltyCoupon;
import cn.aiedge.erp.marketing.mapper.CouponTemplateMapper;
import cn.aiedge.erp.marketing.mapper.LoyaltyCouponMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** {@link CouponRedemptionService} 实现：券核销与回滚，同时维护券模板的「已使用」计数。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponRedemptionServiceImpl implements CouponRedemptionService {

    private final LoyaltyCouponMapper loyaltyCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redeem(Long couponId, Long orderId, String orderNo) {
        LoyaltyCoupon coupon = loyaltyCouponMapper.selectById(couponId);
        if (coupon == null) throw new IllegalArgumentException("优惠券不存在：" + couponId);

        // 幂等：已核销到同一订单 → 直接返回
        if ("USED".equals(coupon.getStatus()) && coupon.getUsedOrderId() != null
                && String.valueOf(coupon.getUsedOrderId()).equals(String.valueOf(orderId))) {
            return;
        }
        if (!"UNUSED".equals(coupon.getStatus())) {
            throw new IllegalArgumentException("优惠券当前状态不可核销：" + coupon.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        int updated = loyaltyCouponMapper.update(null, new LambdaUpdateWrapper<LoyaltyCoupon>()
                .eq(LoyaltyCoupon::getId, couponId)
                .eq(LoyaltyCoupon::getStatus, "UNUSED")   // 并发保护：只有仍为未使用才核销
                .set(LoyaltyCoupon::getStatus, "USED")
                .set(LoyaltyCoupon::getUsedOrderId, orderId)
                .set(LoyaltyCoupon::getUsedTime, now)
                .set(LoyaltyCoupon::getSourceBillNo, orderNo)
                .set(LoyaltyCoupon::getUpdateTime, now));
        if (updated == 0) {
            throw new IllegalArgumentException("优惠券已被其他单据核销，请重新结算");
        }
        incrementUsedCount(coupon.getTemplateId(), 1);
        log.info("[优惠券] 券 {} 已核销至订单 {}（{}）", coupon.getCode(), orderNo, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rollbackByOrder(Long orderId) {
        if (orderId == null) return 0;
        List<LoyaltyCoupon> used = loyaltyCouponMapper.selectList(new LambdaQueryWrapper<LoyaltyCoupon>()
                .eq(LoyaltyCoupon::getUsedOrderId, orderId)
                .eq(LoyaltyCoupon::getStatus, "USED"));
        if (used.isEmpty()) return 0;

        LocalDateTime now = LocalDateTime.now();
        for (LoyaltyCoupon c : used) {
            loyaltyCouponMapper.update(null, new LambdaUpdateWrapper<LoyaltyCoupon>()
                    .eq(LoyaltyCoupon::getId, c.getId())
                    .set(LoyaltyCoupon::getStatus, "UNUSED")
                    .set(LoyaltyCoupon::getUsedOrderId, null)
                    .set(LoyaltyCoupon::getUsedTime, null)
                    .set(LoyaltyCoupon::getUpdateTime, now));
            incrementUsedCount(c.getTemplateId(), -1);
        }
        log.info("[优惠券] 订单 {} 取消/退货，已回滚 {} 张券", orderId, used.size());
        return used.size();
    }

    /** 维护券模板「已使用」计数（回滚时不低于 0） */
    private void incrementUsedCount(Long templateId, int delta) {
        if (templateId == null) return;
        CouponTemplate tpl = couponTemplateMapper.selectById(templateId);
        if (tpl == null) return;
        int used = tpl.getUsedCount() == null ? 0 : tpl.getUsedCount();
        int next = Math.max(used + delta, 0);
        couponTemplateMapper.update(null, new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, templateId)
                .set(CouponTemplate::getUsedCount, next)
                .set(CouponTemplate::getUpdateTime, LocalDateTime.now()));
    }
}
