package cn.aiedge.base.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

/**
 * 租户隔离拦截器（生产级增强版）
 *
 * 修复平台级问题：原生 TenantLineInnerInterceptor 在 getTenantId() 返回 null 时
 * 会注入字面量 `tenant_id = null` 条件（PostgreSQL 中 NULL=NULL 永不匹配），
 * 导致无 Sa-Token 会话的线程（调度器、ApplicationRunner、@Async 异步回调等）
 * 的所有租户表查询静默返回空结果（启动日志曾出现 109 处此类注入）。
 *
 * 本实现：租户不可解析时（未登录/无会话线程/未设置临时租户上下文），
 * 整体跳过租户处理（数据平台级可见，与"无租户上下文即不隔离"语义一致）；
 * 异步线程需要租户上下文时通过 MyBatisPlusConfig.setTempTenantId() 显式设置。
 *
 * @author AI-Ready Team
 */
public class AiReadyTenantLineInnerInterceptor extends TenantLineInnerInterceptor {

    public AiReadyTenantLineInnerInterceptor(TenantLineHandler tenantLineHandler) {
        super(tenantLineHandler);
    }

    /**
     * 应跳过租户处理的两种情况：
     * <ol>
     *   <li><b>租户不可解析</b>：未登录 / 无会话线程 / 未设置临时租户上下文
     *       —— 登录、注册、启动任务等**认证前**链路靠这一条跨租户可查；</li>
     *   <li><b>当前会话整体豁免</b>：平台超级管理员（`SUPER_ADMIN`）。
     *       拦截器本身不认识「超管」，不加这一条，给某张表开了自动注入后超管也会被收敛到自己的会话租户，
     *       表现为「admin 突然看不到别的租户数据」。口径与
     *       `SysUserServiceImpl.resolveScopedTenantId` / `SysUserController.assertSameTenant` 同源。</li>
     * </ol>
     *
     * <p><b>安全边界（动这里之前必读）</b>：第 1 条是 `sys_user` 登录链路的前提——
     * `sys_user` 已从 `IGNORE_TENANT_TABLES` 中移出（原因见 MyBatisPlusConfig 该处注释），
     * 登录时按用户名/手机号/邮箱**跨租户**查账号，靠的正是这里的「未登录即跳过」。
     * 所以<b>不要</b>把它改成「未登录也不跳过」的 fail-closed：那会让登录查不到账号。
     * </p>
     * <p>「未登录请求不得触达业务数据」的防线在<b>上游</b>，不在本方法：
     * ① `SaTokenConfig` 的 SaInterceptor 除白名单外一律要求登录（未登录在 Controller 之前就被拒，
     * 根本走不到 Service/SQL）；② 白名单内的接口必须<b>自行</b>校验归属——例如
     * `SseNotificationController` 用 `StpUtil.getLoginIdByToken` 自校验、
     * `FileAccessController` 用 URL 中的 tenantId 做路径隔离。
     * <b>结论：往 SaTokenConfig 白名单里新增任何接口时，必须同时确认该接口自身有归属校验。</b>
     * </p>
     */
    private boolean shouldSkip() {
        return MyBatisPlusConfig.getCurrentTenantIdValue() == null
                || MyBatisPlusConfig.isTenantScopeExempt();
    }

    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        if (shouldSkip()) {
            return;
        }
        super.processSelect(select, index, sql, obj);
    }

    @Override
    protected void processInsert(Insert insert, int index, String sql, Object obj) {
        if (shouldSkip()) {
            return;
        }
        super.processInsert(insert, index, sql, obj);
    }

    @Override
    protected void processUpdate(Update update, int index, String sql, Object obj) {
        if (shouldSkip()) {
            return;
        }
        super.processUpdate(update, index, sql, obj);
    }

    @Override
    protected void processDelete(Delete delete, int index, String sql, Object obj) {
        if (shouldSkip()) {
            return;
        }
        super.processDelete(delete, index, sql, obj);
    }
}
