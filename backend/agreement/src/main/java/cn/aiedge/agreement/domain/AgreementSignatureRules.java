package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementSignature;
import cn.aiedge.agreement.entity.AgreementVersion;
import cn.aiedge.agreement.enums.AgreementVersionStatus;
import cn.aiedge.common.exception.BusinessException;

import java.util.List;
import java.util.Optional;

/**
 * 签署的合法性判定 —— <b>纯函数</b>（§13.6「自然人代表主体」）。
 *
 * <p>把判定从 Service 里抽出来，是为了让"签署必须带主体与授权依据"这条
 * **能在单测里被逐条断言**，而不是只写在注释里。</p>
 */
public final class AgreementSignatureRules {

    /** 签署类型：版本签署（本期唯一取值）。 */
    public static final String TYPE_VERSION = "VERSION";

    private AgreementSignatureRules() {
    }

    /**
     * 能签的前提：版本还存在、还是待双方确认的草稿、调用方确实是某一端。
     *
     * <p>已生效/已取代/已否决的版本不能再签 —— 要改就发起变更（㉛）。
     * 本方法与 {@code AgreementInvariants.assertVersionMutable} 同口径但**面向签署**：
     * 报错文案要能告诉用户"下一步该点哪里"。</p>
     */
    public static void assertSignable(AgreementVersion version, AgreementPartySide side) {
        if (version == null) {
            throw BusinessException.notFound("协议版本不存在");
        }
        if (!AgreementInvariants.isDraft(version.getStatus())) {
            throw BusinessException.badRequest("这一版当前状态为「"
                    + AgreementVersionStatus.labelOf(version.getStatus())
                    + "」，不能再签署。要改内容请先「发起变更」，由双方在新版本上重新签署");
        }
        if (side == null || side == AgreementPartySide.NONE) {
            throw BusinessException.forbidden("你不是本协议的缔约方，不能代为签署");
        }
    }

    /**
     * 「凭什么代表这家公司」：授权依据**必填**（§13.6）。
     *
     * <p>没有这一条，签署记录就退化成"某个账号点了一下"，
     * 司法上答不出"谁签的、凭什么代表"。凭证号可空，但填了就是多一份举证。</p>
     */
    public static void assertAuthority(String authorityBasis) {
        if (authorityBasis == null || authorityBasis.isBlank()) {
            throw BusinessException.badRequest("请填写签署人的授权依据（凭什么代表这家主体签字）——"
                    + "例如「法定代表人本人」「授权委托书（含授权范围与限额）」；"
                    + "没有授权依据的签署在举证时站不住脚");
        }
    }

    /**
     * 所代表主体必须与该端的缔约主体一致。
     *
     * <p>⚠️ 这条是纵深防御：{@link AgreementPartySide} 已经按会话租户定住了"我是哪一端"，
     * 这里再核对"你勾的代表主体就是那一端的主体" —— 否则会出现"我代表甲方签，
     * 但记录里写的是乙方主体"这种自相矛盾的留痕。</p>
     */
    public static void assertRepresentedParty(Agreement agreement, AgreementPartySide side, Long representedPartyId) {
        if (representedPartyId == null) {
            throw BusinessException.badRequest("请选择你代表哪个主体签署");
        }
        Long expected = side == AgreementPartySide.A ? agreement.getPartyAId() : agreement.getPartyBId();
        if (expected == null) {
            throw BusinessException.badRequest("协议这一端没有登记缔约主体，无法签署；请先补全协议双方主体");
        }
        if (!expected.equals(representedPartyId)) {
            throw BusinessException.badRequest("你选择代表的主体与协议这一端的缔约主体不一致："
                    + "本协议该端的缔约主体是 " + expected + "，你选了 " + representedPartyId
                    + "。请核对后再签署（签署记录要能回答「谁代表哪个主体签的字」）");
        }
    }

    /**
     * 「当前有效的那一条签署记录」：该版该方中 {@code signHash == 当前快照哈希} 的最新一条。
     *
     * <p>为什么不是"最后一条"：内容被改后旧签署的哈希就对不上了，
     * 拿最后一条会把"对不上哈希的废签"当成有效签署 —— 那正好是"确认后被偷改仍算确认齐全"的翻版。</p>
     *
     * @param rows        该版本该方的全部签署记录（任意顺序）
     * @param currentHash 当前快照的哈希（{@code AgreementSnapshot.sha256(version.snapshotJson)}）
     */
    public static Optional<AgreementSignature> effectiveSignature(List<AgreementSignature> rows, String currentHash) {
        if (rows == null || currentHash == null) {
            return Optional.empty();
        }
        AgreementSignature best = null;
        for (AgreementSignature row : rows) {
            if (row.getSignHash() == null || !row.getSignHash().equalsIgnoreCase(currentHash)) {
                continue;
            }
            if (best == null || isLater(row, best)) {
                best = row;
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean isLater(AgreementSignature candidate, AgreementSignature current) {
        if (candidate.getSignedAt() == null) {
            return false;
        }
        if (current.getSignedAt() == null) {
            return true;
        }
        if (candidate.getSignedAt().isAfter(current.getSignedAt())) {
            return true;
        }
        return candidate.getSignedAt().isEqual(current.getSignedAt())
                && candidate.getId() != null && current.getId() != null
                && candidate.getId() > current.getId();
    }
}
