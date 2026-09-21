package cn.aiedge.base.config;

import cn.aiedge.base.security.StpInterfaceImpl;
import cn.aiedge.base.security.TenantHeaderInterceptor;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final StpInterfaceImpl stpInterface;
    private final TenantHeaderInterceptor tenantHeaderInterceptor;

    /**
     * 注册 Sa-Token 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册安全拦截器（检查Token黑名单）
        registry.addInterceptor(new SecurityInterceptor(stpInterface))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/api/auth/login",
                        "/auth/captcha",
                        "/api/auth/captcha",
                        "/auth/captcha/**",
                        "/api/auth/captcha/**",
                        "/auth/check",
                        "/api/auth/check",
                        "/auth/user/login",
                        "/api/user/login",
                        "/api/user/register",
                        "/api/tenant/register",
                        "/api/temp/reset-password",
                        // 商品图片内容访问（供 <img> 直接引用，不经 Authorization 头）
                        "/api/erp/md/image/view/**",
                        // 通用文件访问（证件/附件/头像等 <img> 直接引用）
                        "/api/file/view/**",
                        // C 端商城登录（C 端用户无 Sa-Token 会话，必须匿名可达）
                        "/api/v1/mall/auth/**",
                        // 商城商品浏览（B2B 租户商城：允许游客进店看商品）。
                        // ⚠️ 放行的是**可达性**，不是数据范围 —— 真正的准入在
                        // `MallProductServiceImpl#requireShop()`：先确定「逛的是哪家店」
                        // （会话租户 → 其次 X-Tenant-Id 头），再按该店 `tenant_shop_config.allowGuest`
                        // 决定是否允许游客；未开通商城（无配置行）一律拒绝。
                        // 价格是否下发另由同表的 `guestShowPrice` 控制（已登录买家不受该开关影响）。
                        "/api/v1/mall/products/**",
                        // PDA(仓库端)登录：登录接口若不放行，未登录就取不到 token（死锁，功能不可用）。
                        // 只放行登录本身 —— /api/v1/warehouse/ 下的业务接口仍需登录。
                        // 该接口 2026-09-20 已由桩实现改为真实鉴权（BCrypt + status + 租户校验）。
                        "/api/v1/warehouse/auth/login",
                        // 前端错误上报：只放行上报端点本身（登录页出错也要能上报）。
                        // 注意不能用 /api/error-report/** —— 那会把 /recent、/query、/statistics
                        // 三个管理端查询接口一起放行，未登录即可读错误日志（含堆栈、SQL、类名）。
                        "/api/error-report",
                        // SSE 连接：EventSource 不支持自定义请求头，token 由查询参数传入，
                        // 控制器内部用 StpUtil.getLoginIdByToken 自行校验（见 SseNotificationController）
                        "/api/sse/**",
                        // 外部运力平台回调（无会话；安全由 HMAC 验签 + 时间戳容差 + nonce 防重放保证，见《渠道管理开发文档》§3.4）
                        "/api/dms/channel/callback",
                        "/xxl-job-admin/**",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/favicon.ico",
                        "/error"
                )
                .order(0);

        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin()。
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/api/auth/login",
                        "/auth/captcha",
                        "/api/auth/captcha",
                        "/auth/captcha/**",
                        "/api/auth/captcha/**",
                        "/auth/check",
                        "/api/auth/check",
                        "/auth/user/login",
                        "/api/user/login",
                        "/api/user/register",
                        "/api/tenant/register",
                        "/api/temp/reset-password",
                        // 商品图片内容访问（供 <img> 直接引用，不经 Authorization 头）
                        "/api/erp/md/image/view/**",
                        // 通用文件访问（证件/附件/头像等 <img> 直接引用）
                        "/api/file/view/**",
                        // C 端商城登录（C 端用户无 Sa-Token 会话，必须匿名可达）
                        "/api/v1/mall/auth/**",
                        // 商城商品浏览（B2B 租户商城：允许游客进店看商品）。
                        // ⚠️ 放行的是**可达性**，不是数据范围 —— 真正的准入在
                        // `MallProductServiceImpl#requireShop()`：先确定「逛的是哪家店」
                        // （会话租户 → 其次 X-Tenant-Id 头），再按该店 `tenant_shop_config.allowGuest`
                        // 决定是否允许游客；未开通商城（无配置行）一律拒绝。
                        // 价格是否下发另由同表的 `guestShowPrice` 控制（已登录买家不受该开关影响）。
                        "/api/v1/mall/products/**",
                        // PDA(仓库端)登录：登录接口若不放行，未登录就取不到 token（死锁，功能不可用）。
                        // 只放行登录本身 —— /api/v1/warehouse/ 下的业务接口仍需登录。
                        // 该接口 2026-09-20 已由桩实现改为真实鉴权（BCrypt + status + 租户校验）。
                        "/api/v1/warehouse/auth/login",
                        // 前端错误上报：只放行上报端点本身（登录页出错也要能上报）。
                        // 注意不能用 /api/error-report/** —— 那会把 /recent、/query、/statistics
                        // 三个管理端查询接口一起放行，未登录即可读错误日志（含堆栈、SQL、类名）。
                        "/api/error-report",
                        // SSE 连接：EventSource 不支持自定义请求头，token 由查询参数传入，
                        // 控制器内部用 StpUtil.getLoginIdByToken 自行校验（见 SseNotificationController）
                        "/api/sse/**",
                        // 外部运力平台回调（无会话；安全由 HMAC 验签 + 时间戳容差 + nonce 防重放保证，见《渠道管理开发文档》§3.4）
                        "/api/dms/channel/callback",
                        "/xxl-job-admin/**",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/favicon.ico",
                        "/error"
                )
                .order(1);

        // 租户请求头一致性校验（order=2，排在鉴权之后）：
        // 非超管若携带与会话租户不一致的 X-Tenant-Id → 403。
        // 背景：全仓 20+ 文件、100 余处直接消费该头，而它完全由客户端决定
        // （前端原先还默认回落 '1'），这些查询所在表要么在租户插件忽略清单、要么根本不带
        // tenant_id 条件 ⇒ 多租户插件救不了，头是唯一租户判据。详见 TenantHeaderInterceptor。
        registry.addInterceptor(tenantHeaderInterceptor)
                .addPathPatterns("/**")
                .order(2);
    }
}