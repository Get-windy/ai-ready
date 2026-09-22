package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.Agreement;
import cn.aiedge.agreement.entity.AgreementTermination;
import cn.aiedge.agreement.enums.AgreementStatus;
import cn.aiedge.agreement.enums.AgreementTerminationSource;
import cn.aiedge.agreement.enums.AgreementTerminationStatus;
import cn.aiedge.common.exception.BusinessException;

/**
 * 终止的状态机与边界 —— <b>纯函数</b>（§13.7）。
 *
 * <h3>⚠️ 终止 ≠ 免责（本类的第一条纪律）</h3>
 * 本类与它服务的 {@code agreement_termination} 表**只**处理"停止履行这个事实"。
 * <b>不判定谁违约、不计算赔偿、不结清、不免责</b> —— 那些走 §13.8（平台不裁判）。
 * 因此本类里**没有**任何"责任是否了结"的概念，连字段都没有
 * （迁移 {@code V11.490.0} 的 DO $$ 用列名黑名单在真库上反证这一点）。
 *
 * <h3>五种来源的推进方式</h3>
 * <pre>
 *   协商一致 MUTUAL_AGREEMENT   ：PENDING →（对方确认）CONFIRMED /（对方异议）OBJECTED /（发起方撤回）WITHDRAWN
 *   自然到期 NATURAL_EXPIRY     ：发起即 CONFIRMED
 *   单方终止 UNILATERAL         ：发起即 CONFIRMED（⚠️ 只表示"停止履行"，不代表不违约）
 *   因对方违约 COUNTERPARTY_BREACH：发起即 CONFIRMED（"违约"是当事人主张，不是平台认定）
 *   平台清退 PLATFORM_EXPULSION ：发起即 CONFIRMED（可申诉 = 异议留痕）
 * </pre>
 */
public final class AgreementTerminationRules {

    /** 通知用户的口径（会随 VO 一起下发，界面必须显示）：终止只是停止履行。 */
    public static final String NO_EXEMPTION_NOTICE =
            "终止只表示「从终止之日起停止履行」，系统不会自动结清货款、不会自动免除任何责任；"
                    + "是否存在违约、要不要赔偿，由双方自行协商或通过司法途径解决（平台不裁判）。";

    private AgreementTerminationRules() {
    }

    /**
     * 能否对这个协议发起终止。
     *
     * <ul>
     *   <li>草稿协议：不能终止（还没成立的东西不叫终止，删掉或用「否决」即可）；</li>
     *   <li>已终止：不能重复终止；</li>
     *   <li>平台清退：必须由**系统租户（平台侧）**发起，且平台必须是该协议的一端
     *       （裁定⑥：看不到的协议不能操作，平台对"租户↔租户"协议的清退本期不做）。</li>
     * </ul>
     */
    public static void assertRequestable(Agreement agreement, AgreementTerminationSource source,
                                        AgreementPartySide side, Long sessionTenantId) {
        if (agreement == null) {
            throw BusinessException.notFound("协议不存在或你不是本协议的任一缔约方");
        }
        if (isStatus(agreement, AgreementStatus.TERMINATED)) {
            throw BusinessException.badRequest("该协议已经是终止状态，不需要重复终止");
        }
        if (isStatus(agreement, AgreementStatus.DRAFT)) {
            throw BusinessException.badRequest("该协议还没有生效，谈不上终止；"
                    + "如果不想签了，直接删除草稿或让对方「否决」当前版本即可");
        }
        if (side == null || side == AgreementPartySide.NONE) {
            throw BusinessException.forbidden("你不是本协议的缔约方，不能发起终止");
        }
        if (source == AgreementTerminationSource.PLATFORM_EXPULSION) {
            // 平台侧 = 系统租户 1（平台服务协议的甲方固定为"平台主体 + 系统租户 1"，§12.1）
            if (!Long.valueOf(1L).equals(sessionTenantId)) {
                throw BusinessException.forbidden("平台清退只能由平台方发起；如你认为对方存在违规，"
                        + "请走争议处理或向平台举报，由平台核实后处理");
            }
        }
        if (source == AgreementTerminationSource.NATURAL_EXPIRY) {
            // 自然到期是"时间到了"，由任一方登记事实即可，但要能对上有效期
            if (agreement.getEffectiveTo() == null) {
                throw BusinessException.badRequest("本协议没有约定结束日期（长期有效），"
                        + "不存在「自然到期」；请改用「协商一致」或「单方终止」");
            }
        }
    }

    /** 对方表态（确认 / 异议）的前提：记录还在"待对方确认"，且表态人不是发起方。 */
    public static void assertCounterpartyActionable(AgreementTermination termination, Long operatorId) {
        if (termination == null) {
            throw BusinessException.notFound("终止记录不存在");
        }
        if (statusOf(termination) != AgreementTerminationStatus.PENDING) {
            throw BusinessException.badRequest("这条终止记录当前状态为「"
                    + AgreementTerminationStatus.labelOf(termination.getStatus())
                    + "」，不需要再表态");
        }
        if (termination.getRequestedBy() != null && termination.getRequestedBy().equals(operatorId)) {
            throw BusinessException.forbidden("你是这次终止的发起方，不能替对方确认终止；"
                    + "如需收回请用「撤回终止」");
        }
    }

    /**
     * 已终止的记录上，对方仍可**提异议**（留痕），但**不回滚终止事实**。
     *
     * <p>为什么允许：单方终止本来就是一方就能做的动作，「停止履行」已经发生，
     * 系统不假装它没发生；对方的异议是留给日后主张权利的证据（是否违约走 §13.8）。</p>
     */
    public static void assertObjectable(AgreementTermination termination, Long operatorId) {
        if (termination == null) {
            throw BusinessException.notFound("终止记录不存在");
        }
        AgreementTerminationStatus status = statusOf(termination);
        if (status != AgreementTerminationStatus.PENDING && status != AgreementTerminationStatus.CONFIRMED) {
            throw BusinessException.badRequest("这条终止记录当前状态为「"
                    + AgreementTerminationStatus.labelOf(termination.getStatus()) + "」，不能提异议");
        }
        if (termination.getRequestedBy() != null && termination.getRequestedBy().equals(operatorId)) {
            throw BusinessException.forbidden("你是这次终止的发起方，不能自己给自己提异议");
        }
    }

    /** 发起方撤回的前提：还在"待对方确认"，且撤回人就是发起人。 */
    public static void assertWithdrawable(AgreementTermination termination, Long operatorId) {
        if (termination == null) {
            throw BusinessException.notFound("终止记录不存在");
        }
        if (statusOf(termination) != AgreementTerminationStatus.PENDING) {
            throw BusinessException.badRequest("只有「待对方确认」的终止才能撤回，当前状态为「"
                    + AgreementTerminationStatus.labelOf(termination.getStatus()) + "」");
        }
        if (termination.getRequestedBy() == null || !termination.getRequestedBy().equals(operatorId)) {
            throw BusinessException.forbidden("只有终止的发起人才能撤回这次终止");
        }
    }

    /** 该来源是否"发起即生效"（只有协商一致需要对方确认）。 */
    public static boolean takesEffectImmediately(AgreementTerminationSource source) {
        return source != null && !source.isRequiresCounterpartyConfirm();
    }

    public static AgreementTerminationStatus statusOf(AgreementTermination termination) {
        AgreementTerminationStatus s = AgreementTerminationStatus.of(termination == null ? null : termination.getStatus());
        return s == null ? AgreementTerminationStatus.PENDING : s;
    }

    private static boolean isStatus(Agreement agreement, AgreementStatus expected) {
        return agreement != null && agreement.getStatus() != null && agreement.getStatus() == expected.getCode();
    }
}
