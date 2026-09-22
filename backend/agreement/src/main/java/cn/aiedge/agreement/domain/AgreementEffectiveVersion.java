package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementVersionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 「**这个业务时点，生效的是哪一版**」——纯函数，不依赖 Spring / 数据库，可直接单测。
 *
 * <h3>为什么必须按业务时点取版本（不能读"当前版本"）</h3>
 * ㉛ 定的是"下单时刻生效的那一版"。若读"当前版本"，那么双方今天改一次佣金比例，
 * 昨天已经发生的交易口径会被**追溯篡改** —— 结算算错、举证说不清。
 * 因此 {@code agreement_version} 带 {@code effective_from / effective_to}，
 * 由本类在版本集合里按区间挑那一版。</p>
 *
 * <h3>⚠️ 为什么"曾生效"的版本（SUPERSEDED）也必须参与挑选</h3>
 * 一份协议同时只有一个 ACTIVE 版本 —— 新版本生效时，旧版本在同一事务里被置为
 * SUPERSEDED（{@code uk_agreement_version_active} 只允许一行 status=1）。
 * 于是<b>只看 status=ACTIVE 会导致"回看历史"查不到任何版本</b>：
 * 今天改了协议，昨天那笔单就取不到当时的设定了 —— 恰好违反㉛"不许篡改已发生交易的口径"。
 *
 * <p>所以本类的口径是：<b>参与挑选 = 曾经生效过的版本</b>，即
 * {@code status ∈ {ACTIVE, SUPERSEDED}}</p>
 * <ul>
 *   <li>{@code ACTIVE}：当前生效那一版；</li>
 *   <li>{@code SUPERSEDED}：已被新版取代，但它在自己的
 *       {@code [effective_from, effective_to)} 区间里**确实生效过**；</li>
 *   <li>{@code DRAFT} / {@code REJECTED}：**从未生效**，任何时点都不该按它们执行 —— 排除。</li>
 * </ul>
 *
 * <p>挑选顺序：先在区间内的版本里优先取 ACTIVE；没有 ACTIVE 时取<b>版本号最大</b>的那一版
 * （版本号越大 = 越晚谈定的口径）。这样即便旧版本的 {@code effective_to} 没被回填
 * （历史数据常见），也不会挑到更早的版本。</p>
 *
 * <p>区间判定：{@code (effective_from 为空 或 <= 时点) 且 (effective_to 为空 或 > 时点)}。
 * 空值表示"不设边界"（协议只约定起始、到期日另行签署续签的常见形态）。</p>
 */
public final class AgreementEffectiveVersion {

    private AgreementEffectiveVersion() {
    }

    /** 曾生效过的版本状态集合（见类注释：绝不含 DRAFT / REJECTED）。 */
    public static List<Integer> effectiveStatuses() {
        return List.of(AgreementVersionStatus.ACTIVE.getCode(), AgreementVersionStatus.SUPERSEDED.getCode());
    }

    /**
     * 在给定版本集合里挑出该业务时点生效的那一版。
     *
     * @param versions     同一份协议的版本集合（调用方已按 agreement_id 收窄）
     * @param businessTime **业务时点**（下单/发货/结算发生的时刻）；为 {@code null} 时
     *                     不做区间过滤（供"只想看当前生效版本"的展示场景使用，
     *                     但**执行链路必须传值**，见 {@code AgreementRuntime}）
     * @return 生效版本；没有任何版本落在这个时点则返回空（由调用方转成明确的中文结果，不是 NPE）
     */
    public static Optional<AgreementVersion> pick(Collection<AgreementVersion> versions, LocalDateTime businessTime) {
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }
        List<AgreementVersion> candidates = new ArrayList<>();
        for (AgreementVersion v : versions) {
            if (v == null || isNeverEffective(v.getStatus())) {
                continue;
            }
            if (inRange(v, businessTime)) {
                candidates.add(v);
            }
        }
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        candidates.sort(Comparator
                // ① 当前生效（ACTIVE）优先于已被取代的历史版本
                .comparing((AgreementVersion v) -> !AgreementInvariants.isActive(v.getStatus()))
                // ② 同状态下取版本号最大（版本号越大 = 越晚谈定的口径）
                .thenComparing(v -> v.getVersionNo() == null ? Integer.MIN_VALUE : v.getVersionNo(),
                        Comparator.reverseOrder())
                // ③ 版本号也相同（数据异常）时取 id 大的，保证结果稳定可复现
                .thenComparing(v -> v.getId() == null ? Long.MIN_VALUE : v.getId(), Comparator.reverseOrder()));
        return Optional.of(candidates.get(0));
    }

    /** 该状态是否"从未生效"（草稿 / 被否决）：这类版本任何时点都不该被拿来执行。 */
    public static boolean isNeverEffective(Integer status) {
        return AgreementInvariants.isDraft(status) || AgreementInvariants.isRejected(status);
    }

    /**
     * 该版本的有效期区间是否覆盖业务时点。
     *
     * <p>{@code businessTime} 为 null ⇒ 不过滤（返回 true）。</p>
     */
    public static boolean inRange(AgreementVersion v, LocalDateTime businessTime) {
        if (businessTime == null) {
            return true;
        }
        LocalDateTime from = v.getEffectiveFrom();
        if (from != null && from.isAfter(businessTime)) {
            return false;
        }
        LocalDateTime to = v.getEffectiveTo();
        // 右开区间：到期当刻（含）不再算生效，避免"到期日 23:59:59" 这类边界上的口径争议
        return to == null || businessTime.isBefore(to);
    }
}
