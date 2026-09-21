package cn.aiedge.base.security;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 租户请求头 {@code X-Tenant-Id} 与会话租户的一致性校验。
 *
 * <p><b>为什么需要它</b>：本仓有 20+ 个文件、100 余处直接消费 {@code X-Tenant-Id}
 * （打印模板、报表、数据源、工作流、系统参数等），而这个头**完全由客户端决定** ——
 * 前端无条件注入且默认回落 {@code '1'}（见 {@code pc-admin/src/utils/request.ts}）。
 * 只要改一下 localStorage 或直接 curl 带头，就能读写别的租户（含平台租户）的数据。
 * 这些表要么在租户插件忽略清单里、要么查询里根本不带 {@code tenant_id}，
 * 多租户插件救不了它们 —— 头就是唯一的租户判据。</p>
 *
 * <p><b>口径</b>：</p>
 * <ul>
 *   <li>头缺失或空白 → 放行（不改变既有行为）；</li>
 *   <li>未登录 → 放行（由 {@code SaInterceptor} 负责拒绝，本拦截器不越权处理）；</li>
 *   <li>平台超管（租户隔离豁免）→ 放行（"切换租户"是超管的正常能力）；</li>
 *   <li>无会话租户（如 C 端用户）→ 放行（其数据隔离另有机制，不在本拦截器职责内）；</li>
 *   <li>其余账号：头值 ≠ 会话租户 → **403 拒绝**。<br>
 *       宁可拒绝也不能静默改写：静默改写会让"越权读取"看起来成功，
 *       问题被掩盖到下一次数据比对才暴露。</li>
 * </ul>
 *
 * <p><b>配套</b>：前端已把 {@code || '1'} 的回落去掉，改为发送会话真实租户（2026-09-20）。</p>
 */
@Slf4j
@Component
public class TenantHeaderInterceptor implements HandlerInterceptor {

    /** 与前端 {@code request.ts} 及既有 100 余处消费点保持一致的请求头名 */
    public static final String TENANT_HEADER = "X-Tenant-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String raw = request.getHeader(TENANT_HEADER);
        if (raw == null || raw.isBlank()) {
            return true;
        }
        if (!StpUtil.isLogin()) {
            return true;
        }
        if (MyBatisPlusConfig.isTenantScopeExempt()) {
            return true;
        }
        Long sessionTenantId = MyBatisPlusConfig.getCurrentTenantIdValue();
        if (sessionTenantId == null) {
            return true;
        }

        long requested;
        try {
            requested = Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("非法的 {} 请求头（忽略，交由消费方按既有逻辑处理）: value={}, uri={}",
                    TENANT_HEADER, raw, request.getRequestURI());
            return true;
        }

        if (sessionTenantId != requested) {
            log.warn("拒绝跨租户请求：{}={}, 会话租户={}, uri={}",
                    TENANT_HEADER, requested, sessionTenantId, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"code\":403,\"message\":\"无权访问其他租户的数据\",\"data\":null}");
            return false;
        }
        return true;
    }
}
