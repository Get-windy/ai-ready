package cn.aiedge.base.config;

import cn.dev33.satoken.stp.StpUtil;
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
 * 本实现：**认证前**没有会话的线程（未登录 / 调度器 / @Async 回调 / 未设置临时租户上下文）
 * 整体跳过租户处理（登录、注册等链路需要跨租户查账号）；
 * 异步线程需要租户上下文时通过 MyBatisPlusConfig.setTempTenantId() 显式设置。
 *
 * <p><b>2026-09-21 语义收紧（平台-BREAK-01）</b>：原实现把「未登录」与
 * 「已登录但 session 里没有 tenantId」当成同一件事，两者都跳过注入。后者是登录入口漏写
 * `session.tenantId` 的**缺陷表现**，跳过即等于该会话对所有租户表可读可写（fail-open）。
 * 现在只有「未登录」才跳过；已登录而无租户上下文时注入恒假条件，即 fail-closed。</p>
 *
 * @author AI-Ready Team
 */
public class AiReadyTenantLineInnerInterceptor extends TenantLineInnerInterceptor {

    public AiReadyTenantLineInnerInterceptor(TenantLineHandler tenantLineHandler) {
        super(tenantLineHandler);
    }

    /**
     * 是否跳过租户注入。三种情形，判定顺序即优先级：
     * <p><b>适用范围</b>：只用于 SELECT / UPDATE / DELETE（读与改的过滤口径）。
     * <b>INSERT 不走本方法</b> —— 写路径必须盖章，见 {@link #processInsert}。</p>
     * <ol>
     *   <li><b>当前会话整体豁免</b>：平台超级管理员（`SUPER_ADMIN`）⇒ 跳过。
     *       拦截器本身不认识「超管」，不加这一条，给某张表开了自动注入后超管也会被收敛到自己的会话租户，
     *       表现为「admin 突然看不到别的租户数据」。口径与
     *       `SysUserServiceImpl.resolveScopedTenantId` / `SysUserController.assertSameTenant` 同源。</li>
     *   <li><b>已解析出会话租户</b>（或临时租户上下文）⇒ 不跳过，正常注入。</li>
     *   <li><b>无租户上下文</b> ⇒ 只有**未登录**（认证前链路：登录/注册/启动任务/异步线程）才跳过；
     *       已登录却没有租户**不跳过**，按 fail-closed 处理。<b>这是平台-BREAK-01 的修复点</b>，
     *       理由与代价见方法内注释。</li>
     * </ol>
     *
     * <p><b>安全边界（动这里之前必读）</b>：「未登录即跳过」是 `sys_user` 登录链路的前提——
     * `sys_user` 已从 `IGNORE_TENANT_TABLES` 中移出（原因见 MyBatisPlusConfig 该处注释），
     * 登录时按用户名/手机号/邮箱**跨租户**查账号，靠的正是这一条。
     * 所以<b>不要</b>把「未登录」这一支也改成 fail-closed：那会让登录查不到账号。
     * </p>
     * <p>由此推出一条实现约定：<b>任何登录入口都必须在 `StpUtil.login()` 之后立刻写入
     * `session.tenantId`</b>，且要在同一次请求内任何业务查询之前完成。现有口径见
     * `SysUserServiceImpl#login`（主站）、`PdaAuthController`（仓库端）、
     * `ClientAuthController`（打印端）、`MallAuthServiceImpl#login`（商城 C 端）。
     * 漏写不会再表现为「多看到数据」，而是该会话查什么都为空——排查时先查这里。
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
        // ① 平台超管：显式豁免（全局视野 / 可切换租户），意图明确，保留。
        if (MyBatisPlusConfig.isTenantScopeExempt()) {
            return true;
        }
        // ② 已解析出会话租户：正常注入。
        if (MyBatisPlusConfig.getCurrentTenantIdValue() != null) {
            return false;
        }
        // ③ 没有租户上下文 —— 这里必须区分「认证前」和「认证后却没带租户」，
        //    把两者一视同仁正是平台-BREAK-01 的 fail-open：
        //
        //      · 认证前（未登录）：登录/注册/白名单接口、调度器、@Async 回调等线程里
        //        根本没有会话，**必须跳过**。`sys_user` 已从 IGNORE_TENANT_TABLES 移出，
        //        按用户名/手机号跨租户查账号靠的就是这一条（见下方安全边界注释）。
        //      · 认证后却没有租户：只可能是登录时漏写 `session.tenantId`
        //        （2026-09-20 实测入口：`MallAuthServiceImpl#login`、已被 PDA/打印端沿用），
        //        此时若一并跳过，该会话对本库**所有租户表**都是可读可写的——即 fail-open。
        //        故这里**不跳过**：父类会注入 `tenant_id = null`（PostgreSQL 中恒为 UNKNOWN，
        //        不等同于任何行），效果是 fail-closed：查不到、改不到，而不是全租户可见。
        //
        //    代价（须知悉）：漏写租户的登录入口会表现为「登录成功但列表全空」，
        //    而不是「看到别人的数据」。这是有意的取舍——宁可少数据，不可串租户。
        return !isLoggedInSafely();
    }

    /** 读会话登录态；无 Web 请求上下文（调度器/异步线程）时按「未登录」处理，不抛异常。 */
    private boolean isLoggedInSafely() {
        try {
            return StpUtil.isLogin();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        if (shouldSkip()) {
            return;
        }
        super.processSelect(select, index, sql, obj);
    }

    /**
     * INSERT 的租户盖章**不享受「超管整体豁免」**。
     *
     * <p><b>为什么单独开一条口径（2026-09-26 实测定性）</b>：
     * 写路径上，本仓长期存在一条错误认知 ——「tenant_id 由 `MetaObjectHandler.insertFill` 自动盖章」。
     * 实际不成立：MyBatis 构建语句时，`BaseStatementHandler` 先 `mappedStatement.getBoundSql()`
     * （MP 生成的 `<if test="et.tenantId != null">tenant_id,</if>` 在这一步就**定稿**），
     * 之后才 `newParameterHandler()`；而 MP 的填充发生在 `MybatisParameterHandler` 的构造参数求值里。
     * ⇒ **对没有标 `@TableField(fill = ...)` 的普通字段（`tenantId` 正是），insertFill 的赋值
     * 永远进不了 SQL**。全仓只有 `mall/b2b/model/BaseEntity` 与 `ApiAccessLog` 两处给 tenantId 标了 fill，
     * 其余 439 处能正确落租户的写入**全部是业务代码显式 `setTenantId(...)`**。
     *
     * <p>于是：非超管写入时，父类 `processInsert` 会兜底盖章；而超管走的是 `shouldSkip()` 的第①支
     * （租户隔离整体豁免），**连写也一起跳过了** → 新行落到 `tenant_id` 列默认值 **0**，对自己租户不可见，
     * 全程无报错、无日志。2026-09-18 起 `biz_party`（客户/会员建档，唯一只依赖 fill 的路径）
     * 持续落 0，正是此因；`tools/e2e-marketing.cjs` 的前置守卫因此长期失败。
     *
     * <p>所以这里把「读豁免」和「写盖章」分开：读（SELECT/UPDATE/DELETE）照旧豁免，超管保留全局视野；
     * **写（INSERT）只要有可解析的租户就必须盖章**，解析不出（未登录的种子/登录链路、无租户上下文的
     * 调度线程）才跳过，由调用方显式指定。调用方已显式 `setTenantId` 时，
     * 父类 `ignoreInsert` 会因列已存在而跳过，原值不会被覆盖。
     */
    @Override
    protected void processInsert(Insert insert, int index, String sql, Object obj) {
        if (MyBatisPlusConfig.getCurrentTenantIdValue() == null) {
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
