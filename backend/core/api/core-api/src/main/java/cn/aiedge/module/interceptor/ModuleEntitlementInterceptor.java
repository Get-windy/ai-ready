package cn.aiedge.module.interceptor;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.module.service.ModuleEntitlementService;
import cn.aiedge.permission.annotation.RequirePermission;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 模块 entitlement 门：把「平台方给该租户开通了哪些模块」接进请求链。
 *
 * <p><b>判定链</b>：接口上的 {@code @SaCheckPermission} 权限码 → 归属模块
 * （`sys_module_permission` 最长前缀）→ 该模块是否已给本租户开通 → 未开通则 403。
 * 判定口径见 {@link ModuleEntitlementService}。</p>
 *
 * <p><b>注册顺序（必须是 3）</b>：Sa-Token 的 `SaInterceptor` 排在 order 1，
 * 它 {@code preHandle} 的内部就是「先 {@code SaStrategy.checkMethodAnnotation(...)} 做注解鉴权、
 * 再跑 auth 函数」（已反编译确认），所以 order 3 天然落在 `@SaCheckPermission` **之后** ——
 * 这一点很重要：接口该有权限而用户没有时，报的必须是「无权限访问: xxx」，
 * 而不是被模块门抢先报成「模块未开通」，否则排查方向会被带偏。</p>
 *
 * <p><b>顺序上的已知例外</b>：{@code @RequirePermission} 由 core-api 的 AOP 切面
 * `PermissionAspect` 处理，而 AOP 切的是**方法调用**、永远发生在所有拦截器 `preHandle` 之后
 * ⇒ 这 18 个端点上模块门会**先于**权限检查说话（两边都缺时文案会是「模块未开通」）。
 * 两者都是 403、都真实成立，故未为此把这 18 个端点的判定搬进切面 ——
 * 代价是同一类拒绝在这批端点上文案不同，排查时需知悉。</p>
 *
 * <p><b>三种情况一律放行</b>（本类只做加法，不做减法）：
 * <ol>
 *   <li>非 {@link HandlerMethod}（静态资源、错误页等）；</li>
 *   <li>平台超管（{@code tenantScopeExempt}）—— 超管要能替任何租户排查；</li>
 *   <li>会话里没有租户上下文 —— 此时数据层已被租户拦截器收敛为 fail-closed
 *       （见 `AiReadyTenantLineInnerInterceptor`），本门再补一刀只会让报错更难懂。</li>
 * </ol>
 * 另外，接口没有权限标注（{@code @SaCheckPermission} / {@code @RequirePermission}）时也放行 ——
 * 本门是**按已有权限码挂载的**，没有码就没有"归属模块"可判。裸接口的收敛属 E-01/E-08（补码），不属本门。
 * 覆盖口径实测：`@SaCheckPermission` **1438 个端点** + `@RequirePermission` **18 个**。</p>
 *
 * <p><b>403 语义的区分</b>：文案形如
 * {@code 模块未开通：仓储管理（warehouse），请联系平台管理员}，
 * 与 Sa-Token 的 {@code 无权限访问: <permission>} 在文案上可区分；
 * 两者 HTTP 状态都是 403，前端按 message 区分展示即可。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ModuleEntitlementInterceptor implements HandlerInterceptor {

    private final ModuleEntitlementService entitlementService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (MyBatisPlusConfig.isTenantScopeExempt()) {
            return true;
        }
        Long tenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (tenantId == null) {
            return true;
        }

        Requirement requirement = requiredPermissions(handlerMethod);
        if (requirement.codes().isEmpty()) {
            return true;
        }
        List<String> codes = requirement.codes();

        Optional<ModuleEntitlementService.BlockedModule> blocked =
                entitlementService.firstBlockedModule(tenantId, codes, requirement.requireAll());
        if (blocked.isEmpty()) {
            return true;
        }

        ModuleEntitlementService.BlockedModule m = blocked.get();
        log.warn("模块拦截：租户未开通该模块 tenantId={}, module={}, permissionCodes={}, uri={}",
                tenantId, m.moduleCode(), codes, request.getRequestURI());
        throw BusinessException.forbidden(
                "模块未开通：" + m.moduleName() + "（" + m.moduleCode() + "），请联系平台管理员");
    }

    /** 本次请求要求的权限码 + 它们之间是 AND 还是 OR */
    private record Requirement(List<String> codes, boolean requireAll) {
        static final Requirement NONE = new Requirement(List.of(), true);
    }

    /**
     * 取本处理器要求的权限码：优先方法级，其次类级（两种注解都是方法/类两级都认）。
     *
     * <p><b>认两种注解</b>，因为本仓有两套并存的权限标注（实测端点口径）：
     * <ul>
     *   <li>{@link SaCheckPermission} —— **1438 个端点**，本仓主流。默认 `mode = AND`（已反编译
     *       `SaCheckPermission.class` 确认 `AnnotationDefault: SaMode.AND`）。</li>
     *   <li>{@link RequirePermission} —— 18 个端点，由 core-api 的 `PermissionAspect` 处理。
     *       **默认 `logical = OR`**，与本仓另一套相反，所以必须分开取值。</li>
     * </ul>
     * 两者都没标注时返回空 ⇒ 放行（见类注释：没有码就没有"归属模块"可判）。</p>
     *
     * <p><b>已知不覆盖</b>：**程序化**权限校验（如 core-base 的
     * {@code SecurityContext#checkPermission(code)}）没有可枚举的注解，本门看不见 ——
     * 不是权限被放开，而是模块门对那几个调用点不生效。
     * （crm 曾有一处同类写法 {@code CrmPermissions.require}，2026-09-21 已随该域的
     * E-01 批次统一改成 {@code @SaCheckPermission} 并删掉该类，现在能被本门看见。）</p>
     */
    private Requirement requiredPermissions(HandlerMethod handlerMethod) {
        SaCheckPermission sa = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), SaCheckPermission.class);
        if (sa == null) {
            sa = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), SaCheckPermission.class);
        }
        if (sa != null) {
            List<String> codes = clean(sa.value());
            if (!codes.isEmpty()) {
                return new Requirement(codes, sa.mode() != SaMode.OR);
            }
        }

        RequirePermission rp = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequirePermission.class);
        if (rp == null) {
            rp = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
        }
        if (rp != null) {
            List<String> codes = clean(rp.value());
            if (!codes.isEmpty()) {
                return new Requirement(codes, rp.logical() == RequirePermission.Logical.AND);
            }
        }

        return Requirement.NONE;
    }

    private List<String> clean(String[] raw) {
        if (raw == null || raw.length == 0) {
            return List.of();
        }
        List<String> codes = new ArrayList<>(Arrays.asList(raw));
        codes.removeIf(c -> c == null || c.isBlank());
        return codes;
    }
}
