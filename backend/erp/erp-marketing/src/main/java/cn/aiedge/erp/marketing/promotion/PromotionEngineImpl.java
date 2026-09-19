package cn.aiedge.erp.marketing.promotion;

import cn.aiedge.erp.marketing.entity.CouponTemplate;
import cn.aiedge.erp.marketing.entity.LoyaltyCoupon;
import cn.aiedge.erp.marketing.entity.PromoActivity;
import cn.aiedge.erp.marketing.mapper.CouponTemplateMapper;
import cn.aiedge.erp.marketing.mapper.LoyaltyCouponMapper;
import cn.aiedge.erp.marketing.mapper.PromoActivityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 促销引擎实现。算法口径见 {@link PromotionEngine} 接口注释。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionEngineImpl implements PromotionEngine {

    private final PromoActivityMapper promoActivityMapper;
    private final LoyaltyCouponMapper loyaltyCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String EXCLUSIVE = "EXCLUSIVE";

    @Override
    @Transactional(readOnly = true)
    public PromotionResult evaluate(PromotionRequest req) {
        PromotionResult result = new PromotionResult();
        if (req == null || req.getLines() == null || req.getLines().isEmpty()) return result;

        List<CartLine> lines = req.getLines();
        BigDecimal orderAmount = lines.stream().map(CartLine::lineAmount).reduce(ZERO, BigDecimal::add);

        // 已被行级优惠「消耗」的行号（条目消耗：一行只吃一个行级优惠）
        Set<Integer> consumedLines = new HashSet<>();

        // ── 1. 促销活动 ──
        List<PromoActivity> candidates = loadCandidates(req);
        for (PromoActivity act : candidates) {
            if (!matchScope(act, req)) {
                result.getSkipped().add(act.getName() + "：使用范围/促销客户不匹配");
                continue;
            }
            if (!matchQuota(act, req)) {
                result.getSkipped().add(act.getName() + "：活动次数已达上限");
                continue;
            }

            List<CartLine> matched = matchLines(act, lines);
            if (matched.isEmpty()) {
                result.getSkipped().add(act.getName() + "：无匹配商品");
                continue;
            }

            // 条目消耗：行级活动只看未被占用的行
            if (isItemLevel(act)) {
                matched = matched.stream().filter(l -> !consumedLines.contains(l.getLineNo())).toList();
                if (matched.isEmpty()) {
                    result.getSkipped().add(act.getName() + "：匹配商品已被更高优先级活动占用（条目消耗）");
                    continue;
                }
            }

            BigDecimal raw = computeDiscount(act, matched, orderAmount);
            if (raw.compareTo(ZERO) <= 0 && !isGift(act)) {
                result.getSkipped().add(act.getName() + "：未达门槛或优惠为 0");
                continue;
            }
            BigDecimal capped = capByActivity(act, raw);

            List<PromotionAllocation> allocs = allocate(act, matched, lines, capped, orderAmount);
            result.setPromoDiscount(result.getPromoDiscount().add(capped));
            result.getAppliedPromotionIds().add(act.getId());
            result.getNotes().add(act.getName() + "（" + act.getPromoMode() + "）优惠 "
                    + capped.setScale(2, RoundingMode.HALF_UP)
                    + (isGift(act) ? "；满赠仅登记赠品，不计金额优惠" : ""));

            if (isItemLevel(act)) {
                matched.forEach(l -> consumedLines.add(l.getLineNo()));
            }
            applyAllocations(result, allocs);

            // 独占：命中后不再计算更低优先级活动
            if (EXCLUSIVE.equalsIgnoreCase(act.getStackPolicy())) {
                result.getNotes().add("活动「" + act.getName() + "」为独占（EXCLUSIVE），更低优先级活动不再参与计算");
                break;
            }
        }

        // ── 2. 优惠券 ──
        for (Long couponId : safe(req.getCouponIds())) {
            LoyaltyCoupon coupon = loyaltyCouponMapper.selectById(couponId);
            if (coupon == null) {
                result.getSkipped().add("券 #" + couponId + "：不存在");
                continue;
            }
            String invalid = validateCoupon(coupon, req, orderAmount);
            if (invalid != null) {
                result.getSkipped().add("券 " + coupon.getCode() + "：" + invalid);
                continue;
            }
            BigDecimal face = nz(coupon.getFaceValue());
            BigDecimal usable = face.min(orderAmount.subtract(result.totalDiscount()).max(ZERO));
            if (usable.compareTo(ZERO) <= 0) {
                result.getSkipped().add("券 " + coupon.getCode() + "：可抵扣金额为 0（订单已无剩余金额）");
                continue;
            }
            result.setCouponDiscount(result.getCouponDiscount().add(usable));
            result.getAppliedCouponIds().add(couponId);

            PromotionAllocation a = new PromotionAllocation();
            a.setPromoMode("优惠券");
            a.setScopeType("ORDER");
            a.setCouponId(couponId);
            a.setCouponCode(coupon.getCode());
            a.setDiscountAmount(usable);
            a.setRemark("优惠券核销");
            applyAllocations(result, List.of(a));
        }

        result.setPromoDiscount(scale(result.getPromoDiscount()));
        result.setCouponDiscount(scale(result.getCouponDiscount()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordUsage(List<Long> promotionIds) {
        if (promotionIds == null || promotionIds.isEmpty()) return;
        for (Long id : promotionIds) {
            PromoActivity act = promoActivityMapper.selectById(id);
            if (act == null) continue;
            promoActivityMapper.updateById(act.setQuotaUsed(nzInt(act.getQuotaUsed()) + 1));
        }
        log.debug("[促销引擎] 活动使用次数累加：{}", promotionIds);
    }

    // ═══════════ 载入与过滤 ═══════════

    /** 候选活动：生效中 + 时间覆盖 + 促销方式属引擎支持的三类 */
    private List<PromoActivity> loadCandidates(PromotionRequest req) {
        LocalDate date = req.getOrderDate() == null ? LocalDate.now() : req.getOrderDate();
        LocalDateTime at = date.atTime(23, 59, 59);
        LocalDateTime from = date.atStartOfDay();
        LambdaQueryWrapper<PromoActivity> w = new LambdaQueryWrapper<PromoActivity>()
                .in(PromoActivity::getType, List.of("PRODUCT", "ORDER", "SPECIAL_PRICE"))
                .eq(PromoActivity::getStatus, "published")
                .and(x -> x.isNull(PromoActivity::getStartTime).or().le(PromoActivity::getStartTime, at))
                .and(x -> x.isNull(PromoActivity::getEndTime).or().ge(PromoActivity::getEndTime, from))
                // 优先级降序；同优先级按 id 升序兜底（避免"同优先级随机选择"）
                .orderByDesc(PromoActivity::getPriority)
                .orderByAsc(PromoActivity::getId);
        return promoActivityMapper.selectList(w);
    }

    /** 使用范围（线上线下）与促销客户匹配 */
    private boolean matchScope(PromoActivity act, PromotionRequest req) {
        String scope = act.getPromoScope();
        if (scope != null && !scope.isBlank()) {
            boolean mall = "MALL".equalsIgnoreCase(req.getChannel());
            if (mall && "线下使用".equals(scope)) return false;
            if (!mall && "商城使用".equals(scope)) return false;
        }
        String customers = act.getCustomerIds();
        if (customers != null && !customers.isBlank()) {
            if (req.getCustomerId() == null) return false;
            // 雪花 id 一律按字符串比对，避免超出 2^53 后的精度丢失去重/误判
            return Arrays.stream(customers.split(","))
                    .map(String::trim)
                    .anyMatch(s -> s.equals(String.valueOf(req.getCustomerId())));
        }
        return true;
    }

    private boolean matchQuota(PromoActivity act, PromotionRequest req) {
        if (act.getQuotaTotal() != null && nzInt(act.getQuotaUsed()) >= act.getQuotaTotal()) return false;
        if (act.getQuotaPerCustomer() != null && req.getCustomerId() != null) {
            // 每客户次数在开单侧无法廉价统计（需按客户聚合历史分摊明细），此处只做「已用 >= 每客户上限」的保守拦截
            return nzInt(act.getQuotaUsed()) < act.getQuotaPerCustomer();
        }
        return true;
    }

    /** 行级活动（商品促销 / 特价）按促销商品过滤；整单促销不按商品过滤 */
    private boolean isItemLevel(PromoActivity act) {
        return "PRODUCT".equals(act.getType()) || "SPECIAL_PRICE".equals(act.getType());
    }

    private List<CartLine> matchLines(PromoActivity act, List<CartLine> lines) {
        if (!isItemLevel(act)) return lines;
        Set<String> ids = splitIds(act.getProductIds());
        if (ids.isEmpty()) return lines;
        return lines.stream()
                .filter(l -> l.getProductId() != null && ids.contains(String.valueOf(l.getProductId())))
                .toList();
    }

    // ═══════════ 折扣计算 ═══════════

    private boolean isGift(PromoActivity act) {
        return "满赠".equals(act.getPromoMode()) || "赠品".equals(act.getPromoMode());
    }

    /**
     * 计算活动优惠（未封顶）：
     * · 特价  → Σ max(0, (行成交价 - 特价单价)) × 数量
     * · 打折  → Σ 匹配行金额 × (1 - discountRate)
     * · 满减  → 门槛（minAmount）满足时减 reductionAmount（门槛缺省＝0，即无条件减）
     * · 满赠  → 0（不计金额优惠）
     */
    private BigDecimal computeDiscount(PromoActivity act, List<CartLine> matched, BigDecimal orderAmount) {
        String mode = act.getPromoMode();
        if (isGift(act)) return ZERO;

        if ("特价".equals(mode) && act.getPromoPrice() != null) {
            BigDecimal diff = ZERO;
            for (CartLine l : matched) {
                BigDecimal cut = nz(l.getUnitPrice()).subtract(act.getPromoPrice());
                if (cut.compareTo(ZERO) > 0) diff = diff.add(cut.multiply(nz(l.getQuantity())));
            }
            return diff;
        }

        BigDecimal matchedAmount = matched.stream().map(CartLine::lineAmount).reduce(ZERO, BigDecimal::add);

        if ("打折".equals(mode) && act.getDiscountRate() != null) {
            BigDecimal rate = act.getDiscountRate();
            // 允许两种录入口径：0.9（折扣率）或 90（百分比）
            if (rate.compareTo(BigDecimal.ONE) > 0) rate = rate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            return matchedAmount.multiply(BigDecimal.ONE.subtract(rate));
        }

        if ("满减".equals(mode) && act.getReductionAmount() != null) {
            BigDecimal base = isItemLevel(act) ? matchedAmount : orderAmount;
            BigDecimal threshold = nz(act.getMinAmount());
            if (base.compareTo(threshold) >= 0) return act.getReductionAmount();
            return ZERO;
        }
        return ZERO;
    }

    private BigDecimal capByActivity(PromoActivity act, BigDecimal raw) {
        BigDecimal capped = raw.max(ZERO);
        if (act.getMaxDiscountAmount() != null && capped.compareTo(act.getMaxDiscountAmount()) > 0) {
            return act.getMaxDiscountAmount();
        }
        return capped;
    }

    // ═══════════ 分摊 ═══════════

    /**
     * 优惠分摊：
     * · 行级活动（商品促销/特价）→ 摊到匹配行，按各匹配行金额占比
     * · 整单级活动 / 优惠券 → 按**全单各行金额占比**摊到每一行（SAP / Commerce Layer 同法）
     */
    private List<PromotionAllocation> allocate(PromoActivity act, List<CartLine> matched, List<CartLine> allLines,
                                               BigDecimal amount, BigDecimal orderAmount) {
        List<PromotionAllocation> out = new ArrayList<>();
        if (isGift(act)) {
            // 满赠：登记赠品（赠品 id 取促销商品首项，数量按规则原文不可解析，故 Quantity 留 1 并在备注注明）
            PromotionAllocation g = new PromotionAllocation();
            g.setPromotionId(act.getId());
            g.setPromotionName(act.getName());
            g.setPromoMode(act.getPromoMode());
            g.setScopeType("ITEM");
            g.setGiftQuantity(BigDecimal.ONE);
            g.setRemark("满赠命中：" + (act.getDescription() == null ? "" : act.getDescription()));
            out.add(g);
            return out;
        }

        List<CartLine> basis = isItemLevel(act) ? matched : allLines;
        BigDecimal basisTotal = isItemLevel(act)
                ? matched.stream().map(CartLine::lineAmount).reduce(ZERO, BigDecimal::add)
                : orderAmount;
        if (basisTotal.compareTo(ZERO) <= 0) {
            PromotionAllocation one = new PromotionAllocation();
            one.setPromotionId(act.getId());
            one.setPromotionName(act.getName());
            one.setPromoMode(act.getPromoMode());
            one.setScopeType(isItemLevel(act) ? "ITEM" : "ORDER");
            one.setDiscountAmount(amount);
            one.setRemark("无有效行金额，整笔计入首行");
            out.add(one);
            return out;
        }

        BigDecimal allocated = ZERO;
        for (int i = 0; i < basis.size(); i++) {
            CartLine l = basis.get(i);
            BigDecimal part = (i == basis.size() - 1)
                    ? amount.subtract(allocated)                       // 末行吃掉舍入残差，保证合计 = amount
                    : amount.multiply(l.lineAmount()).divide(basisTotal, 2, RoundingMode.HALF_UP);
            allocated = allocated.add(part);
            PromotionAllocation a = new PromotionAllocation();
            a.setPromotionId(act.getId());
            a.setPromotionName(act.getName());
            a.setPromoMode(act.getPromoMode());
            a.setScopeType(isItemLevel(act) ? "ITEM" : "ORDER");
            a.setLineNo(l.getLineNo());
            a.setProductId(l.getProductId());
            a.setDiscountAmount(part);
            out.add(a);
        }
        return out;
    }

    /** 把分摊结果并入总结果：同 (活动/券, 行) 累加，避免多段分摊产生重复行 */
    private void applyAllocations(PromotionResult result, List<PromotionAllocation> allocs) {
        for (PromotionAllocation a : allocs) {
            Optional<PromotionAllocation> exist = result.getAllocations().stream()
                    .filter(x -> Objects.equals(x.getPromotionId(), a.getPromotionId())
                            && Objects.equals(x.getCouponId(), a.getCouponId())
                            && Objects.equals(x.getLineNo(), a.getLineNo()))
                    .findFirst();
            if (exist.isPresent()) {
                PromotionAllocation e = exist.get();
                e.setDiscountAmount(nz(e.getDiscountAmount()).add(nz(a.getDiscountAmount())));
            } else {
                result.getAllocations().add(a);
            }
        }
    }

    // ═══════════ 券 ═══════════

    /** 券可用性校验；返回 null = 可用，否则返回不可用原因 */
    private String validateCoupon(LoyaltyCoupon coupon, PromotionRequest req, BigDecimal orderAmount) {
        if (!"UNUSED".equals(coupon.getStatus())) return "状态不可用（" + coupon.getStatus() + "）";
        LocalDate date = req.getOrderDate() == null ? LocalDate.now() : req.getOrderDate();
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().toLocalDate().isBefore(date)) {
            return "已过期";
        }
        if (coupon.getReceiveTime() != null && coupon.getReceiveTime().toLocalDate().isAfter(date)) {
            return "尚未到生效日";
        }
        if (req.getCustomerId() != null && coupon.getPartnerId() != null
                && !String.valueOf(coupon.getPartnerId()).equals(String.valueOf(req.getCustomerId()))) {
            return "非本客户所有";
        }
        if (coupon.getPartnerId() == null && req.getCustomerId() == null) {
            return "未绑定客户，无法核销";
        }
        // 门店/线下使用通道：券模板的线下开关
        if (coupon.getTemplateId() != null) {
            CouponTemplate tpl = couponTemplateMapper.selectById(coupon.getTemplateId());
            if (tpl != null) {
                boolean offline = !"MALL".equalsIgnoreCase(req.getChannel());
                if (offline && Integer.valueOf(0).equals(tpl.getOfflineEnabled())) return "该券禁止线下使用";
                if (!offline && Integer.valueOf(0).equals(tpl.getMallEnabled())) return "该券禁止商城使用";
                if ("VOID".equals(tpl.getStatus())) return "券模板已作废";
                BigDecimal threshold = parseThreshold(tpl.getUseRule());
                if (threshold.compareTo(ZERO) > 0 && orderAmount.compareTo(threshold) < 0) {
                    return "未达使用门槛（满 " + threshold.stripTrailingZeros().toPlainString() + " 元可用）";
                }
            }
        }
        return null;
    }

    /** 使用规则 → 门槛金额：UNLIMITED=0；FULL_100=100；（兼容纯数字） */
    private BigDecimal parseThreshold(String useRule) {
        if (useRule == null || useRule.isBlank() || "UNLIMITED".equals(useRule)) return ZERO;
        if (useRule.startsWith("FULL_")) {
            try {
                return new BigDecimal(useRule.substring(5));
            } catch (NumberFormatException e) {
                return ZERO;
            }
        }
        try {
            return new BigDecimal(useRule);
        } catch (NumberFormatException e) {
            return ZERO;
        }
    }

    // ═══════════ 工具 ═══════════

    private static BigDecimal nz(BigDecimal v) { return v == null ? ZERO : v; }

    private static int nzInt(Integer v) { return v == null ? 0 : v; }

    private static BigDecimal scale(BigDecimal v) { return nz(v).setScale(2, RoundingMode.HALF_UP); }

    private static <T> List<T> safe(List<T> list) { return list == null ? List.of() : list; }

    private static Set<String> splitIds(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null || csv.isBlank()) return out;
        for (String s : csv.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
