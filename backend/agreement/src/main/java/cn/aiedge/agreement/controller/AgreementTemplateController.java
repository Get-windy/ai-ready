package cn.aiedge.agreement.controller;

import cn.aiedge.agreement.dto.AgreementFromTemplateDTO;
import cn.aiedge.agreement.dto.AgreementTemplateDTO;
import cn.aiedge.agreement.dto.AgreementTemplateDetailVO;
import cn.aiedge.agreement.dto.AgreementTemplateQuery;
import cn.aiedge.agreement.dto.AgreementTemplateVO;
import cn.aiedge.agreement.service.AgreementTemplateService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.common.utils.Result;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 契约模板接口（§13.9）。
 *
 * <h3>接口契约（下次写前端按这张表来）</h3>
 * <pre>
 * GET    /api/agreement/templates/page?current=&size=&scope=&agreementType=&keyword=&status=
 * GET    /api/agreement/templates/{id}
 * POST   /api/agreement/templates
 * PUT    /api/agreement/templates/{id}
 * DELETE /api/agreement/templates/{id}
 * POST   /api/agreement/templates/{id}/apply      从模板发起契约
 * </pre>
 *
 * <h3>两类权限码（OR）</h3>
 * 读 / 写用「租户侧码」与「平台侧码」**或**的关系：
 * 平台侧拿的是 {@code agreement:platform:template:read} / {@code agreement:platform:template:manage}
 * （归「系统」模块，只开给系统租户），租户侧拿的是 {@code agreement:template:*}。
 * 这样一来"平台合规抽查读"不必新造机制，靠码的归属就把平台/租户分开了（同裁定⑤ 的思路）。</p>
 *
 * <h3>⚠️ {@code platformSide} 由服务端判定，不接受前端传</h3>
 * 否则租户只要自报"我是平台"就能读到别家租户的模板条款。
 * 判定 = 超管（会话标记）或系统租户（tenant 1）。</p>
 *
 * <h3>⚠️ 模板不是默认值</h3>
 * {@code /apply} 只是把模板内容**预填**到新草稿版本，并把"基于模板 X"记进版本快照（司法可追溯）；
 * 是否成为约定，仍取决于双方在那一版上的确认与双签。{@code AgreementRuntime} 绝不读模板。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/agreement/templates")
@RequiredArgsConstructor
@Tag(name = "协议契约模板", description = "平台级 / 租户级契约模板：预填条款、设定与文字；平台可合规抽查读")
public class AgreementTemplateController {

    private final AgreementTemplateService templateService;

    @SaCheckPermission(value = {"agreement:template:list", "agreement:platform:template:read"}, mode = SaMode.OR)
    @GetMapping("/page")
    @Operation(summary = "模板列表（平台可读全部含租户模板；租户只读平台模板+自己的）")
    public Result<Page<AgreementTemplateVO>> page(AgreementTemplateQuery query) {
        return Result.success(templateService.page(query, currentTenant(), platformSide()));
    }

    @SaCheckPermission(value = {"agreement:template:list", "agreement:platform:template:read"}, mode = SaMode.OR)
    @GetMapping("/{id}")
    @Operation(summary = "模板详情（含预填的条款 / 设定 / 文字；⚠️ 预填不等于已约定）")
    public Result<AgreementTemplateDetailVO> detail(@PathVariable Long id) {
        return Result.success(templateService.detail(id, currentTenant(), platformSide()));
    }

    @SaCheckPermission(value = {"agreement:template:create", "agreement:platform:template:manage"}, mode = SaMode.OR)
    @PostMapping
    @Operation(summary = "新建模板（平台模板只有平台侧能建）")
    public Result<Long> create(@Valid @RequestBody AgreementTemplateDTO dto) {
        return Result.success(templateService.create(dto, currentTenant(), platformSide(), operator()));
    }

    @SaCheckPermission(value = {"agreement:template:update", "agreement:platform:template:manage"}, mode = SaMode.OR)
    @PutMapping("/{id}")
    @Operation(summary = "修改模板（平台能读租户模板但不能改；三份内容清单不传=不动、传空数组=清空）")
    public Result<Void> update(@PathVariable Long id, @RequestBody AgreementTemplateDTO dto) {
        templateService.update(id, dto, currentTenant(), platformSide(), operator());
        return Result.success();
    }

    @SaCheckPermission(value = {"agreement:template:delete", "agreement:platform:template:manage"}, mode = SaMode.OR)
    @DeleteMapping("/{id}")
    @Operation(summary = "删除模板（软删；已基于它发起的协议不受影响）")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id, currentTenant(), platformSide());
        return Result.success();
    }

    /**
     * 从模板发起契约。
     *
     * <p>权限用 **AND**：既要有"能建协议"（{@code agreement:create}），
     * 也要有"能用模板"（{@code agreement:template:apply}）——
     * 否则只给了模板查看权的人就能绕开协议创建的门。</p>
     */
    @SaCheckPermission(value = {"agreement:create", "agreement:template:apply"}, mode = SaMode.AND)
    @PostMapping("/{id}/apply")
    @Operation(summary = "从模板发起契约（预填内容 + 把模板来源记进版本快照；预填≠已约定，仍需双方确认）")
    public Result<Long> apply(@PathVariable Long id, @Valid @RequestBody AgreementFromTemplateDTO dto) {
        return Result.success(templateService.apply(id, dto, currentTenant(), platformSide(), operator()));
    }

    // ── 内部 ──

    private Long currentTenant() {
        return SecurityUtils.getCurrentTenantId();
    }

    private Long operator() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 当前会话是否平台侧。
     *
     * <p>判定口径与 {@code MyBatisPlusConfig.isTenantScopeExempt()} 同源：超管，或系统租户（1）。
     * 在 HTTP 层可以安全地实时判角色（拦截器里不行 —— 那里实时判角色会无限递归，本仓实踩过）。</p>
     */
    private boolean platformSide() {
        Long tenantId = currentTenant();
        return SecurityUtils.hasRole("SUPER_ADMIN") || Long.valueOf(1L).equals(tenantId);
    }
}
