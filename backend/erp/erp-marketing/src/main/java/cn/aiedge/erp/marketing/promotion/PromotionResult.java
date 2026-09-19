package cn.aiedge.erp.marketing.promotion;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 促销引擎输出：一次结算的全部优惠结果 */
@Data
public class PromotionResult {

    /** 促销优惠合计（不含券） */
    private BigDecimal promoDiscount = BigDecimal.ZERO;
    /** 优惠券优惠合计 */
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    /** 命中的活动 id（已按优先级顺序） */
    private List<Long> appliedPromotionIds = new ArrayList<>();
    /** 命中的券 id（已核销或待核销） */
    private List<Long> appliedCouponIds = new ArrayList<>();
    /** 行级分摊明细 */
    private List<PromotionAllocation> allocations = new ArrayList<>();
    /** 未生效/被跳过原因（排障与页面提示用，如「已被更高优先级活动占用」「未达门槛」） */
    private List<String> skipped = new ArrayList<>();
    /** 已生效活动的计算说明（与 skipped 严格分开，便于前端区分「生效了什么」与「为什么没生效」） */
    private List<String> notes = new ArrayList<>();

    /** 优惠合计 = 促销 + 券 */
    public BigDecimal totalDiscount() {
        return nz(promoDiscount).add(nz(couponDiscount));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
