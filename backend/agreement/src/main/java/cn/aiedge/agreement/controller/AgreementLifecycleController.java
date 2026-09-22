package cn.aiedge.agreement.controller;

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
import cn.aiedge.agreement.service.AgreementLifecycleService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.utils.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 协议的**成立过程与终止**接口：发起 → 唯一送达 → 多轮协商 → 签署 → 终止（§13.4 ~ §13.7）。
 *
 * <h3>接口契约（前端照这张表写）</h3>
 * <pre>
 * POST /api/agreement/lifecycle/{agreementId}/invites                发起唯一送达（**仅此一次**返回明文 token/短链）
 * GET  /api/agreement/lifecycle/{agreementId}/invites                邀请记录（发给谁、状态、留痕；不含明文 token）
 * GET  /api/agreement/lifecycle/invites/{inviteId}                   单条邀请详情
 * POST /api/agreement/lifecycle/invites/open                         受邀方领取并查看（body: token 或 inviteCode）
 * POST /api/agreement/lifecycle/invites/{inviteId}/revoke            发起方撤回邀请
 * POST /api/agreement/lifecycle/{agreementId}/proposals              协商提案（反要约 = 新建 DRAFT 版本）
 * GET  /api/agreement/lifecycle/version/{versionId}/diff             版本 diff（不传 against 则对比上一版）
 * POST /api/agreement/lifecycle/version/{versionId}/sign             签署（自然人代表主体）
 * GET  /api/agreement/lifecycle/version/{versionId}/signatures       该版本的签署记录
 * POST /api/agreement/lifecycle/{agreementId}/terminations           发起终止
 * GET  /api/agreement/lifecycle/{agreementId}/terminations           终止记录
 * POST /api/agreement/lifecycle/terminations/{id}/confirm            对方确认终止
 * POST /api/agreement/lifecycle/terminations/{id}/object             对方提异议
 * POST /api/agreement/lifecycle/terminations/{id}/withdraw           发起方撤回终止
 * </pre>
 *
 * <h3>三条口径</h3>
 * <ol>
 *   <li><b>「本方是谁」一律由服务端按会话租户判定</b>，不接受前端传"我是甲方还是乙方" ——
 *       否则谁都能声称自己是对方（与既有的 confirm 同口径）；</li>
 *   <li><b>邀请必须先登录</b>：打开邀请的接口在进入业务前显式判登录，未登录给一句能看懂的
 *       中文提示，而不是兜底成 401 系统错误（§13.5 硬要求）；</li>
 *   <li><b>二维码不在后端生成</b>：本仓没有二维码依赖，后端只返回可供前端渲染的短链与邀请码 ——
 *       不为"生成图片"引入 zxing 之类的新依赖。</li>
 * </ol>
 *
 * <p>⚠️ 错误一律 {@code BusinessException} + 中文白话（用 RuntimeException 会被兜底 advice
 * 吞成「系统异常，请稍后重试」，把"这份契约不是发给你的"这种可自解的问题误导成服务故障）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/agreement/lifecycle")
@RequiredArgsConstructor
@Tag(name = "协议成立过程与终止", description = "唯一送达 / 多轮协商（反要约） / 签署 / 终止（终止≠免责）")
public class AgreementLifecycleController {

    private final AgreementLifecycleService lifecycleService;

    // ══════════════════════ 唯一送达（§13.5） ══════════════════════

    /**
     * 发起唯一送达。
     *
     * <p>权限码用 {@code agreement:invite:create}（新码，与 V11.491.0 同批落库）。</p>
     */
    @SaCheckPermission("agreement:invite:create")
    @PostMapping("/{agreementId}/invites")
    @Operation(summary = "发起唯一送达（收件方由服务端判定为「另一端」；仅此响应返回明文 token 与短链）")
    public Result<AgreementInviteVO> createInvite(@PathVariable Long agreementId,
                                                  @RequestBody(required = false) AgreementInviteCreateDTO dto) {
        return Result.success(lifecycleService.createInvite(agreementId,
                dto == null ? new AgreementInviteCreateDTO() : dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:view")
    @GetMapping("/{agreementId}/invites")
    @Operation(summary = "邀请记录（发给谁、是否已领取/已撤回/已过期、查看与领取留痕）")
    public Result<List<AgreementInviteVO>> listInvites(@PathVariable Long agreementId) {
        return Result.success(lifecycleService.listInvites(agreementId, currentTenant()));
    }

    @SaCheckPermission("agreement:view")
    @GetMapping("/invites/{inviteId}")
    @Operation(summary = "单条邀请详情（不含明文 token）")
    public Result<AgreementInviteVO> inviteDetail(@PathVariable Long inviteId) {
        return Result.success(lifecycleService.inviteDetail(inviteId, currentTenant()));
    }

    /**
     * 受邀方**领取并查看**。
     *
     * <p>⚠️ 「必须先登录」在这里显式把关：未登录时给一句中文白话，
     * 而不是让 Sa-Token 或者兜底 advice 抛一个看不懂的错。</p>
     */
    @SaCheckPermission("agreement:invite:accept")
    @PostMapping("/invites/open")
    @Operation(summary = "受邀方领取并查看（token 或邀请码二选一；租户或代表主体不匹配一律拒绝）")
    public Result<AgreementInviteVO> openInvite(@RequestBody AgreementInviteOpenDTO dto) {
        if (!StpUtil.isLogin() && !SecurityUtils.isLoggedIn()) {
            throw BusinessException.unauthorized("请先登录后再打开这份契约邀请——"
                    + "契约只发给指定的那一家，登录后我们才能确认是发给你本人的");
        }
        return Result.success(lifecycleService.openInvite(dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:invite:revoke")
    @PostMapping("/invites/{inviteId}/revoke")
    @Operation(summary = "撤回邀请（撤回后链接立即失效；与「已过期」「已领取」是三种不同的失效）")
    public Result<AgreementInviteVO> revokeInvite(@PathVariable Long inviteId,
                                                 @Parameter(description = "撤回原因")
                                                 @RequestParam(required = false) String reason) {
        return Result.success(lifecycleService.revokeInvite(inviteId, reason, currentTenant(), operator()));
    }

    // ══════════════════════ 多轮协商（§13.4） ══════════════════════

    /**
     * 协商提案（反要约）。
     *
     * <p>权限码**复用** {@code agreement:version:create}：反要约就是"发起变更"的一种
     * （在对方提的那一版上改），语义与操作都是同一个动作的两个入口，
     * 因此不另造权限码（少一个码 = 少一处漏授）。</p>
     */
    @SaCheckPermission("agreement:version:create")
    @PostMapping("/{agreementId}/proposals")
    @Operation(summary = "协商提案/反要约：新建一个草稿版本（现行生效版本在协商期间继续有效）")
    public Result<Long> propose(@PathVariable Long agreementId, @Valid @RequestBody AgreementProposalDTO dto) {
        return Result.success(lifecycleService.propose(agreementId, dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:view")
    @GetMapping("/version/{versionId}/diff")
    @Operation(summary = "版本差异（条款/设定/文字/履约方式逐条对比；不传 againstVersionId 则对比上一版）")
    public Result<AgreementVersionDiffVO> diff(@PathVariable Long versionId,
                                              @Parameter(description = "对比基准版本 ID；不传 = 上一版")
                                              @RequestParam(required = false) Long againstVersionId) {
        return Result.success(lifecycleService.diff(versionId, againstVersionId, currentTenant()));
    }

    // ══════════════════════ 签署（§13.6） ══════════════════════

    @SaCheckPermission("agreement:sign")
    @PostMapping("/version/{versionId}/sign")
    @Operation(summary = "签署（必须填「代表哪个主体」与「凭什么代表」；同时完成本方确认）")
    public Result<AgreementSignatureVO> sign(@PathVariable Long versionId,
                                            @RequestBody AgreementSignDTO dto) {
        return Result.success(lifecycleService.sign(versionId, dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:view")
    @GetMapping("/version/{versionId}/signatures")
    @Operation(summary = "该版本的签署记录（谁·代表哪个主体·在哪个租户·凭什么代表·签了哪一版·何时·签章哈希）")
    public Result<List<AgreementSignatureVO>> listSignatures(@PathVariable Long versionId) {
        return Result.success(lifecycleService.listSignatures(versionId, currentTenant()));
    }

    // ══════════════════════ 终止（§13.7） ══════════════════════

    @SaCheckPermission("agreement:terminate:request")
    @PostMapping("/{agreementId}/terminations")
    @Operation(summary = "发起终止（五种来源；协商一致需对方确认，其余发起即停止履行）")
    public Result<AgreementTerminationVO> requestTermination(@PathVariable Long agreementId,
                                                            @Valid @RequestBody AgreementTerminationCreateDTO dto) {
        return Result.success(lifecycleService.requestTermination(agreementId, dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:view")
    @GetMapping("/{agreementId}/terminations")
    @Operation(summary = "终止记录（含依据、是否主张违约、对方异议；⚠️ 终止≠免责）")
    public Result<List<AgreementTerminationVO>> listTerminations(@PathVariable Long agreementId) {
        return Result.success(lifecycleService.listTerminations(agreementId, currentTenant()));
    }

    @SaCheckPermission("agreement:terminate:confirm")
    @PostMapping("/terminations/{id}/confirm")
    @Operation(summary = "对方确认终止（协商一致 → 协议置为已终止 = 停止履行）")
    public Result<AgreementTerminationVO> confirmTermination(
            @PathVariable Long id, @RequestBody(required = false) AgreementTerminationActionDTO dto) {
        return Result.success(lifecycleService.confirmTermination(id, dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:terminate:confirm")
    @PostMapping("/terminations/{id}/object")
    @Operation(summary = "对方提异议（待确认时提 ⇒ 终止不成立；已终止后提 ⇒ 只留痕，不回滚终止事实）")
    public Result<AgreementTerminationVO> objectTermination(
            @PathVariable Long id, @RequestBody(required = false) AgreementTerminationActionDTO dto) {
        return Result.success(lifecycleService.objectTermination(id, dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:terminate:request")
    @PostMapping("/terminations/{id}/withdraw")
    @Operation(summary = "发起方撤回终止（仅「待对方确认」可撤）")
    public Result<AgreementTerminationVO> withdrawTermination(
            @PathVariable Long id, @RequestBody(required = false) AgreementTerminationActionDTO dto) {
        return Result.success(lifecycleService.withdrawTermination(id, dto, currentTenant(), operator()));
    }

    // ── 内部：会话上下文 ──

    /** 登录会话租户（登录时写入 Sa-Token Session；协议各表不参与租户拦截器，身份全靠它）。 */
    private Long currentTenant() {
        return SecurityUtils.getCurrentTenantId();
    }

    private Long operator() {
        return StpUtil.getLoginIdAsLong();
    }
}
