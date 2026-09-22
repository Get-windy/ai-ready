package cn.aiedge.agreement.service;

import cn.aiedge.agreement.dto.AgreementInviteCreateDTO;
import cn.aiedge.agreement.dto.AgreementInviteOpenDTO;
import cn.aiedge.agreement.dto.AgreementInviteVO;
import cn.aiedge.agreement.dto.AgreementProposalDTO;
import cn.aiedge.agreement.dto.AgreementSignDTO;
import cn.aiedge.agreement.dto.AgreementSignatureVO;
import cn.aiedge.agreement.dto.AgreementTerminationActionDTO;
import cn.aiedge.agreement.dto.AgreementTerminationCreateDTO;
import cn.aiedge.agreement.dto.AgreementTerminationVO;
import cn.aiedge.agreement.dto.AgreementVersionDiffVO;

import java.util.List;

/**
 * 协议的**成立过程与终止**（§13.4 ~ §13.7）。
 *
 * <pre>
 *   发起 → 唯一送达 → 多轮协商（要约 / 反要约）→ 签署 → 生效
 *   有效期 · 阶段修改（变更单语义）· 终止（单方终止 ≠ 免责）
 * </pre>
 *
 * <h3>四条纪律（每条都对应文档里的一句话）</h3>
 * <ol>
 *   <li><b>唯一送达</b>（§13.5）：token 绑定"目标主体 + 目标租户 + 目标版本 + 时效 + 一次性"，
 *       身份不匹配一律回「这份契约不是发给你的」（{@code AgreementInviteGuard} 是唯一判定处）；</li>
 *   <li><b>协商期间现行版本继续有效</b>（㉛）：反要约只新增/否决草稿，绝不触碰既有生效版本与主档状态；</li>
 *   <li><b>签署 = 自然人代表主体</b>（§13.6）：复用既有"确认"链路再落签署记录，不造两套确认机制；</li>
 *   <li><b>终止 ≠ 免责</b>（§13.7）：只记录「停止履行」这个事实，**不结清、不免责**。</li>
 * </ol>
 *
 * <p>会话租户一律由调用方（Controller）从 Sa-Token Session 取值传入，<b>不接受前端参数</b>：
 * 协议这三张新表都不参与租户拦截器（裁定⑥），可见性/身份判定全靠它。</p>
 */
public interface AgreementLifecycleService {

    // ══════════════════════ 唯一送达（§13.5） ══════════════════════

    /**
     * 发起唯一送达：给**另一端**生成一次性邀请（返回明文 token / 邀请码 / 短链）。
     *
     * <p>同一目标、同一版本下若已有"待领取"的邀请，会被自动作废（"唯一送达"不允许两个有效链接并存）。</p>
     */
    AgreementInviteVO createInvite(Long agreementId, AgreementInviteCreateDTO dto,
                                  Long sessionTenantId, Long operatorId);

    /** 某协议的邀请记录（发给谁、状态、留痕）。**不下发明文 token**。 */
    List<AgreementInviteVO> listInvites(Long agreementId, Long sessionTenantId);

    /** 单条邀请详情（按 id，仅协议双方可见）。**不下发明文 token**。 */
    AgreementInviteVO inviteDetail(Long inviteId, Long sessionTenantId);

    /**
     * 受邀方**领取并查看**：先过五绑定校验（必须先登录 + 会话租户与所代表主体都要匹配），
     * 通过后留痕（谁/何时/哪个渠道）并置为"已领取"（一次性）。
     */
    AgreementInviteVO openInvite(AgreementInviteOpenDTO dto, Long sessionTenantId, Long operatorId);

    /** 发起方撤回邀请（撤回后立即失效，区分于"过期"与"已使用"）。 */
    AgreementInviteVO revokeInvite(Long inviteId, String reason, Long sessionTenantId, Long operatorId);

    // ══════════════════════ 多轮协商（§13.4） ══════════════════════

    /**
     * 协商提案（**反要约**）：新建一个 DRAFT 版本。
     *
     * <p>⚠️ 现行生效版本在协商期间**继续有效**（㉛：谈成之前交易照常）——
     * 本方法不碰既有 ACTIVE 版本，也不改主档状态。</p>
     */
    Long propose(Long agreementId, AgreementProposalDTO dto, Long sessionTenantId, Long operatorId);

    /**
     * 版本差异（"本版 vs 上一版"）：不传 {@code againstVersionId} 时自动对比上一版；
     * 首版没有基准时，差异里全部标为"新增"。
     */
    AgreementVersionDiffVO diff(Long versionId, Long againstVersionId, Long sessionTenantId);

    // ══════════════════════ 签署（§13.6） ══════════════════════

    /**
     * 签署：**自然人代表主体**。
     *
     * <p>先复用既有的"本方确认"链路（同一套双签痕迹），再落一条签署记录 ——
     * 因此不存在第二套确认机制，库里的双签痕迹仍是"能否置为生效"的唯一依据。</p>
     */
    AgreementSignatureVO sign(Long versionId, AgreementSignDTO dto, Long sessionTenantId, Long operatorId);

    /** 某版本的签署记录（含"这条对当前内容是否仍有效"的判定）。 */
    List<AgreementSignatureVO> listSignatures(Long versionId, Long sessionTenantId);

    // ══════════════════════ 终止（§13.7） ══════════════════════

    /** 发起终止（五种来源；协商一致需对方确认，其余四种发起即生效）。 */
    AgreementTerminationVO requestTermination(Long agreementId, AgreementTerminationCreateDTO dto,
                                             Long sessionTenantId, Long operatorId);

    /** 某协议的终止记录（倒序）。 */
    List<AgreementTerminationVO> listTerminations(Long agreementId, Long sessionTenantId);

    /** 对方**确认**终止（协商一致 → 已终止；协议主档随之置为已终止）。 */
    AgreementTerminationVO confirmTermination(Long terminationId, AgreementTerminationActionDTO dto,
                                             Long sessionTenantId, Long operatorId);

    /**
     * 对方**提异议**。
     *
     * <p>待对方确认时提 ⇒ 终止不成立（协议继续有效）；
     * 已终止后提 ⇒ 只留痕，**不回滚终止事实**（是否违约走 §13.8，平台不裁判）；</p>
     */
    AgreementTerminationVO objectTermination(Long terminationId, AgreementTerminationActionDTO dto,
                                            Long sessionTenantId, Long operatorId);

    /** 发起方**撤回**终止（仅"待对方确认"可撤）。 */
    AgreementTerminationVO withdrawTermination(Long terminationId, AgreementTerminationActionDTO dto,
                                              Long sessionTenantId, Long operatorId);
}
