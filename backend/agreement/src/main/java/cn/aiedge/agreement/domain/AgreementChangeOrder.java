package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.common.exception.BusinessException;

/**
 * 变更单语义（§13.7 阶段修改）—— <b>纯函数</b>。
 *
 * <h3>阶段修改（履约中变更）的三件事</h3>
 * <ol>
 *   <li><b>只能"新版本 + 双方签署"</b>：变更 = 新建 DRAFT 版本，再走双签与生效（㉛）；</li>
 *   <li><b>关联原版本</b>：必须能回答"这一版是从哪一版改出来的"（{@code origin_version_id}）；</li>
 *   <li><b>不追溯</b>：已发生的单据与结算不受影响（第三条通用原则）——
 *       这句声明要写进版本行（{@code no_retroactive_note}），随版本一起被双方看到、一起被举证。</li>
 * </ol>
 *
 * <p>为什么"不追溯"要做成**版本行上的显式声明**而不是流程说明：
 * 履约中改协议最常见的纠纷就是"新价从哪天算"。把口径写在版本上，日后核对时
 * 一眼就能看到"这一版从生效之日起适用，不追溯"，而不是靠人回忆当时怎么谈的。</p>
 */
public final class AgreementChangeOrder {

    /**
     * "本变更不追溯"的固定声明文本。
     *
     * <p>固定文本而不是让用户自由填写：这是一条**系统口径**，不是协商内容；
     * 自由填写会出现"这次追溯、那次不追溯"，反而让口径不可预期。</p>
     */
    public static final String NO_RETROACTIVE_NOTE =
            "本变更自生效之日起适用于新发生的业务，不追溯变更前已发生的单据与结算。";

    private AgreementChangeOrder() {
    }

    /**
     * 校验变更单的基准版本合法：必须属于同一协议、必须是一个**真实存在**的版本。
     *
     * <p>⚠️ 特别地：基准版本不能是"新版本自己"（先有鸡还是先有蛋），
     * 也必须是同一个协议下的版本 —— 否则会出现"A 协议的变更单基于 B 协议的版本"，
     * 举证链条直接断掉。</p>
     */
    public static void assertBaseVersion(Long agreementId, AgreementVersion base) {
        if (base == null) {
            throw BusinessException.badRequest("找不到变更的基准版本，请刷新后重试");
        }
        if (agreementId == null || !agreementId.equals(base.getAgreementId())) {
            throw BusinessException.badRequest("基准版本不属于本协议，不能作为变更的依据");
        }
    }

    /** 是否能对这份协议发起变更（已终止的不能改，只能重签一份新的）。 */
    public static void assertChangeable(boolean terminated) {
        if (terminated) {
            throw BusinessException.badRequest("已终止的协议不能再发起变更；如需继续合作请重新签订一份协议");
        }
    }
}
