package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.AgreementTerm;
import cn.aiedge.agreement.entity.AgreementTermOption;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.common.exception.BusinessException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 协议的状态机与三条不变量 —— <b>纯函数，不依赖 Spring / 数据库，可直接单测</b>。
 *
 * <p>把不变量写在这里而不是散在 Service 的 if 里，是为了让"注释里说的"变成"能被断言的行为"：
 * {@code backend/agreement/src/test/java/cn/aiedge/agreement/AgreementInvariantsTest.java}
 * 逐条覆盖本类的每个拒绝分支。</p>
 *
 * <p>三条不变量（§十二 12.2）：<b>已生效版本只读</b> · <b>双签缺一不可</b> · <b>必填条款没选完不许生效</b>。</p>
 */
public final class AgreementInvariants {

    /** 法务判定违法的选项：不许被选中并签署生效（§3.4.4d0 校验点 3 —— 所有协议不得违法是最高约束）。 */
    public static final String LEGAL_REJECTED = "REJECTED";

    private AgreementInvariants() {
    }

    // ══════════════════════ 不变量 1：已生效版本只读 ══════════════════════

    /**
     * 已生效版本（ACTIVE）的条款与快照一律只读：改协议只能新建 DRAFT 版本（㉛ / §3.4.4d2 规定 1）。
     *
     * <p>为什么这条必须硬拦：若允许改已生效版本，就会出现"一方偷偷改了佣金比例，
     * 对方还在按老条款发货" —— 既是商业纠纷，也让系统无法自证清白（举证失败）。</p>
     */
    public static void assertVersionMutable(AgreementVersion version) {
        if (version == null) {
            throw BusinessException.notFound("协议版本不存在");
        }
        if (isActive(version.getStatus())) {
            throw BusinessException.badRequest(
                    "已生效的协议版本不可修改：改协议只能发起变更、由双方重新确认后生成新版本（现行版本在谈成之前继续有效）");
        }
        if (isSuperseded(version.getStatus())) {
            throw BusinessException.badRequest("该版本已被新版取代，属历史快照，不可修改");
        }
        if (isRejected(version.getStatus())) {
            throw BusinessException.badRequest("该版本已被否决，不可再修改；如需继续洽谈请发起新的变更");
        }
    }

    /** 该版本是否处于"已生效、快照冻结"状态。 */
    public static boolean isActive(Integer status) {
        return AgreementVersionStatus.ACTIVE.getCode() == (status == null ? -1 : status);
    }

    public static boolean isDraft(Integer status) {
        return AgreementVersionStatus.DRAFT.getCode() == (status == null ? -1 : status);
    }

    public static boolean isSuperseded(Integer status) {
        return AgreementVersionStatus.SUPERSEDED.getCode() == (status == null ? -1 : status);
    }

    public static boolean isRejected(Integer status) {
        return AgreementVersionStatus.REJECTED.getCode() == (status == null ? -1 : status);
    }

    // ══════════════════════ 不变量 2：双签缺一不可 ══════════════════════

    /**
     * 双签校验：**甲乙双方的确认痕迹都必须非空**才允许置 ACTIVE
     * （裁定③ / §3.4.4d2 规定 2）。这是"不能单方改协议"的技术保证，不是流程约定。
     *
     * <p>痕迹 = 确认人 + 时间 + 内容哈希。三者缺一都不算确认。</p>
     */
    public static void assertBothSidesSigned(AgreementVersion version) {
        List<String> missing = new ArrayList<>();
        if (isMissing(version.getPartyAConfirmedBy()) || version.getPartyAConfirmedAt() == null
                || isMissing(version.getPartyASignHash())) {
            missing.add("甲方");
        }
        if (isMissing(version.getPartyBConfirmedBy()) || version.getPartyBConfirmedAt() == null
                || isMissing(version.getPartyBSignHash())) {
            missing.add("乙方");
        }
        if (!missing.isEmpty()) {
            throw BusinessException.badRequest(
                    "还不能生效：下列缔约方尚未确认签署 —— " + String.join("、", missing)
                            + "。协议必须双方都确认（不能单方变更），请等对方确认后再置为生效");
        }
    }

    // ══════════════════════ 不变量 3：必填条款没选完不许生效 ══════════════════════

    /**
     * 由字典派生出"必填条款类别"清单：只要某个 termCode 下有任一启用选项标了
     * {@code required = true}，该条款类别就是必填（与前端 {@code isRequiredGroup} 同口径）。
     */
    public static Set<String> requiredTermCodes(List<AgreementTermOption> options) {
        Set<String> codes = new LinkedHashSet<>();
        if (options == null) {
            return codes;
        }
        for (AgreementTermOption o : options) {
            if (Boolean.TRUE.equals(o.getRequired()) && o.getTermCode() != null && isEnabled(o.getStatus())) {
                codes.add(o.getTermCode());
            }
        }
        return codes;
    }

    /** 必填但**还没约定**的条款类别（§3.4.4d1：平台不设默认值，未约定就必须拦住生效）。 */
    public static List<String> missingRequiredTermCodes(List<AgreementTerm> terms, Set<String> requiredTermCodes) {
        List<String> missing = new ArrayList<>();
        if (requiredTermCodes == null) {
            return missing;
        }
        for (String code : requiredTermCodes) {
            if (findAgreedTerm(terms, code).isEmpty()) {
                missing.add(code);
            }
        }
        return missing;
    }

    /**
     * 已选条款里"需要参数但参数没填"的项（如选了"按比例分账"却没填比例）。
     * 参数名用于给用户一句能看懂的话。
     */
    public static List<String> missingParams(List<AgreementTerm> terms, List<AgreementTermOption> options) {
        List<String> missing = new ArrayList<>();
        if (terms == null) {
            return missing;
        }
        for (AgreementTerm t : terms) {
            AgreementTermOption opt = optionOf(options, t.getTermCode(), t.getOptionCode());
            if (opt == null || !hasText(opt.getNeedsParam())) {
                continue;
            }
            if (!hasText(t.getParamValue())) {
                missing.add(opt.getOptionLabel() + "（还差" + opt.getNeedsParam() + "）");
            }
        }
        return missing;
    }

    /**
     * 置 ACTIVE 前的合并校验：必填条款齐 + 参数齐 + 双签齐，缺任何一项都把**缺什么**说清楚。
     *
     * <p>文案逐条列出而非"条件不满足"，因为这条错误会被双方看到，含糊的话谁也修不了。</p>
     */
    public static void assertActivatable(AgreementVersion version,
                                         List<String> missingRequiredTermCodes,
                                         List<String> missingParams) {
        if (version == null) {
            throw BusinessException.notFound("协议版本不存在");
        }
        if (!isDraft(version.getStatus())) {
            throw BusinessException.badRequest(
                    "只有「待双方确认」的版本才能置为生效，当前版本状态为："
                            + AgreementVersionStatus.labelOf(version.getStatus()));
        }
        List<String> blockers = new ArrayList<>();
        if (missingRequiredTermCodes != null && !missingRequiredTermCodes.isEmpty()) {
            blockers.add("必填条款尚未约定：" + String.join("、", missingRequiredTermCodes));
        }
        if (missingParams != null && !missingParams.isEmpty()) {
            blockers.add("已选条款的附带参数未填：" + String.join("、", missingParams));
        }
        if (!blockers.isEmpty()) {
            blockers.add("平台不提供默认值 —— 未约定的条款必须由双方明确选定后才能生效");
            throw BusinessException.badRequest("还不能生效：" + String.join("；", blockers));
        }
        assertBothSidesSigned(version);
    }

    // ══════════════════════ 拒绝：现行版本继续有效 ══════════════════════

    /**
     * 一方拒绝：只有 DRAFT 版本能被否决。
     *
     * <p>⚠️ 本方法只管版本本身；"**现行版本继续有效、交易照常按现行版本执行**"
     * 由调用方保证（不触碰既有 ACTIVE 版本）—— 这是最容易做错的点，已有单测覆盖。</p>
     */
    public static void assertRejectable(AgreementVersion version) {
        if (version == null) {
            throw BusinessException.notFound("协议版本不存在");
        }
        if (!isDraft(version.getStatus())) {
            throw BusinessException.badRequest(
                    "只有「待双方确认」的版本才能被否决，当前版本状态为："
                            + AgreementVersionStatus.labelOf(version.getStatus()));
        }
    }

    // ══════════════════════ 条款选择的合法性 ══════════════════════

    /**
     * 条款选择必须落在字典里、且该选项可被新协议使用：
     * <ul>
     *   <li>字典里不存在该 (termCode, optionCode) ⇒ 拒绝（自由文本无法自动执行、举证也弱，§3.4.4d）；</li>
     *   <li>选项已停用 ⇒ 拒绝（停用只影响以后新签的协议，历史快照不受影响）；</li>
     *   <li>选项被法务判定违法（legalReviewStatus = REJECTED）⇒ 拒绝（§3.4.4d0）。</li>
     * </ul>
     */
    public static void assertTermsSelectable(List<AgreementTerm> selection, List<AgreementTermOption> options) {
        if (selection == null || selection.isEmpty()) {
            return;
        }
        for (AgreementTerm t : selection) {
            AgreementTermOption opt = optionOf(options, t.getTermCode(), t.getOptionCode());
            if (opt == null) {
                throw BusinessException.badRequest("条款「" + t.getTermCode() + "」选择的选项「"
                        + t.getOptionCode() + "」不在平台条款字典里，请从下拉里重新选择");
            }
            if (!isEnabled(opt.getStatus())) {
                throw BusinessException.badRequest("条款「" + opt.getOptionLabel()
                        + "」已在平台字典里停用，新签协议不能再选它（历史协议快照不受影响）");
            }
            if (LEGAL_REJECTED.equalsIgnoreCase(opt.getLegalReviewStatus())) {
                throw BusinessException.badRequest("条款「" + opt.getOptionLabel()
                        + "」未通过法务审核（已被判定为不合法），不能签署生效。《消费者权益保护法》"
                        + "第二十六条与《民法典》第四百九十六至四百九十八条项下的格式条款不得排除或限制对方权利");
            }
        }
    }

    // ══════════════════════ 「未约定」就不自动执行（§3.4.4d1） ══════════════════════

    /**
     * 按条款类别找"生效版本里到底约定了什么"。
     *
     * <p><b>查不到就返回空 —— 绝不回落到任何平台默认值。</b>
     * 这是 §3.4.4d1 的硬口径：未约定 ⇒ 不自动执行任何赔偿/扣款、挂人工，
     * 而不是拿平台默认去判（平台给默认值就等于平台替双方做了决定）。</p>
     *
     * <p>将来接单据/结算（阶段 B/C）时，需要用"当时生效版本"的条款做判定的地方
     * <b>一律走本方法</b>，并用 {@code Optional.isPresent()} 区分"约定了"与"未约定"
     * —— 不许写 {@code orElse(某默认)}。</p>
     */
    public static Optional<AgreementTerm> findAgreedTerm(List<AgreementTerm> terms, String termCode) {
        if (terms == null || termCode == null) {
            return Optional.empty();
        }
        for (AgreementTerm t : terms) {
            if (termCode.equals(t.getTermCode()) && hasText(t.getOptionCode())) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    // ══════════════════════ 小工具 ══════════════════════

    /** 选项是否启用（1=启用 / 0=停用）。 */
    public static boolean isEnabled(Integer status) {
        return status == null || status == 1;
    }

    private static AgreementTermOption optionOf(List<AgreementTermOption> options, String termCode, String optionCode) {
        if (options == null || termCode == null || optionCode == null) {
            return null;
        }
        for (AgreementTermOption o : options) {
            if (termCode.equals(o.getTermCode()) && optionCode.equals(o.getOptionCode())) {
                return o;
            }
        }
        return null;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /** 「痕迹缺失」判定：null 或空字符串都算没留痕（确认人 / 时间 / 哈希三者缺一都算未确认）。 */
    private static boolean isMissing(Object o) {
        if (o == null) {
            return true;
        }
        return o instanceof String s && s.isBlank();
    }
}
