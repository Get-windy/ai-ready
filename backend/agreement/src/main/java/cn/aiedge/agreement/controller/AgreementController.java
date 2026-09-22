package cn.aiedge.agreement.controller;

import cn.aiedge.agreement.dto.AgreementCreateDTO;
import cn.aiedge.agreement.dto.AgreementQuery;
import cn.aiedge.agreement.dto.AgreementSaveResultVO;
import cn.aiedge.agreement.dto.AgreementUpdateDTO;
import cn.aiedge.agreement.dto.AgreementVO;
import cn.aiedge.agreement.dto.AgreementVersionVO;
import cn.aiedge.agreement.dto.VersionCreateDTO;
import cn.aiedge.agreement.service.AgreementService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.utils.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 协议主档与版本接口。
 *
 * <h3>接口契约（已与前端对齐，不得变更）</h3>
 * <pre>
 * GET    /api/agreement/page?current=&size=&agreementScope=&agreementType=&status=&keyword=
 *        （agreementScope 为**新增可选参数**：TENANT=租户级三类 / PLATFORM=仅平台服务协议 /
 *          不传=不加类型条件；同时传 agreementType 时以 agreementType 为准。返回结构未变）
 * GET    /api/agreement/{id}
 * POST   /api/agreement
 * PUT    /api/agreement/{id}          （terms 是**唯一**写入条款的入口）
 * DELETE /api/agreement/{id}
 * GET    /api/agreement/{id}/versions
 * POST   /api/agreement/{id}/versions
 * GET    /api/agreement/version/{versionId}
 * POST   /api/agreement/version/{versionId}/confirm
 * POST   /api/agreement/version/{versionId}/activate
 * </pre>
 *
 * <h3>「本方」的判定</h3>
 * 由**登录会话租户**与两端 {@code partyATenantId} / {@code partyBTenantId} 比对得出
 * （{@code AgreementPartySide}）。<b>不接受前端传"我是甲方还是乙方"</b> ——
 * 否则任何人都能声称自己是对方并替对方确认，双签就形同虚设。
 *
 * <h3>⚠️ 错误一律用 BusinessException</h3>
 * 不用 RuntimeException：后者会被兜底 advice 吞成 HTTP 500「系统异常，请稍后重试」，
 * 把"必填条款没选完"这种可自解的问题误导成"服务故障"（本仓实踩）。
 */
@Slf4j
@RestController
@RequestMapping("/api/agreement")
@RequiredArgsConstructor
@Tag(name = "协议管理", description = "租户之间 / 租户与平台之间的协议：主档、版本快照、结构化条款、双签")
public class AgreementController {

    private final AgreementService agreementService;

    @SaCheckPermission("agreement:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询协议（只返回本租户是其中一端的协议）")
    public Result<Page<AgreementVO>> page(AgreementQuery query) {
        return Result.success(agreementService.page(query, currentTenant()));
    }

    @SaCheckPermission("agreement:view")
    @GetMapping("/{id}")
    @Operation(summary = "协议详情（含当前生效版本与条款）")
    public Result<AgreementVO> detail(@PathVariable Long id) {
        return Result.success(agreementService.detail(id, currentTenant()));
    }

    @SaCheckPermission("agreement:create")
    @PostMapping
    @Operation(summary = "新建协议草稿（自动生成协议编号与首个草稿版本）")
    public Result<Long> create(@Valid @RequestBody AgreementCreateDTO dto) {
        return Result.success(agreementService.create(dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:update")
    @PutMapping("/{id}")
    @Operation(summary = "修改协议草稿（terms 写到 versionId 指定的草稿版本；回执会列出还缺哪些必填项）")
    public Result<AgreementSaveResultVO> update(@PathVariable Long id, @RequestBody AgreementUpdateDTO dto) {
        return Result.success(agreementService.update(id, dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除协议草稿（仅洽谈中可删）")
    public Result<Void> delete(@PathVariable Long id) {
        agreementService.delete(id, currentTenant());
        return Result.success();
    }

    @SaCheckPermission("agreement:view")
    @GetMapping("/{id}/versions")
    @Operation(summary = "协议版本历史（含草稿与历史版本，各带条款明细与快照原文）")
    public Result<List<AgreementVersionVO>> listVersions(@PathVariable Long id) {
        return Result.success(agreementService.listVersions(id, currentTenant()));
    }

    @SaCheckPermission("agreement:version:create")
    @PostMapping("/{id}/versions")
    @Operation(summary = "发起变更：从现行生效版本复制出新草稿版本（谈成之前现行版本继续有效）")
    public Result<Long> createVersion(@PathVariable Long id, @Valid @RequestBody VersionCreateDTO dto) {
        return Result.success(agreementService.createVersion(id, dto, currentTenant(), operator()));
    }

    @SaCheckPermission("agreement:view")
    @GetMapping("/version/{versionId}")
    @Operation(summary = "单个版本详情（含快照原文，可取证）")
    public Result<AgreementVersionVO> versionDetail(@PathVariable Long versionId) {
        return Result.success(agreementService.versionDetail(versionId, currentTenant()));
    }

    @SaCheckPermission("agreement:version:confirm")
    @PostMapping("/version/{versionId}/confirm")
    @Operation(summary = "本方确认签署（双签之一；本方由会话租户判定，不接收前端传入的身份）")
    public Result<Void> confirm(@PathVariable Long versionId) {
        agreementService.confirm(versionId, currentTenant(), operator());
        return Result.success();
    }

    /**
     * 一方否决该草稿版本。
     *
     * <p><b>本端点是契约之外的**新增**（不是修改）</b>，理由：§3.4.4d2 的变更流程里
     * 「对方拒绝 → DRAFT 置 REJECTED，现行版本继续有效」是**必须成立**的一条语义，
     * 但定死的 14 个端点里没有任何入口能触发它 —— 没有入口的不变量等于没实现。
     * 权限码**复用** {@code agreement:version:confirm}（确认与否决是同一个"表态"动作的两个分支），
     * 因此权限码族仍然是 10 条，不多不少。</p>
     */
    @SaCheckPermission("agreement:version:confirm")
    @PostMapping("/version/{versionId}/reject")
    @Operation(summary = "否决该草稿版本（现行生效版本继续有效，交易照常）")
    public Result<Void> reject(@PathVariable Long versionId,
                               @Parameter(description = "否决原因") @RequestParam(required = false) String reason) {
        agreementService.reject(versionId, reason, currentTenant(), operator());
        return Result.success();
    }

    @SaCheckPermission("agreement:version:activate")
    @PostMapping("/version/{versionId}/activate")
    @Operation(summary = "置为生效（校验必填条款齐 + 参数齐 + 双签齐 + 确认的即当前快照）")
    public Result<Void> activate(@PathVariable Long versionId) {
        agreementService.activate(versionId, currentTenant(), operator());
        return Result.success();
    }

    // ── 内部：会话上下文 ──

    /** 登录会话租户（登录时写入 Sa-Token Session；协议四表不参与租户拦截器，可见性全靠它）。 */
    private Long currentTenant() {
        return SecurityUtils.getCurrentTenantId();
    }

    private Long operator() {
        return StpUtil.getLoginIdAsLong();
    }
}
