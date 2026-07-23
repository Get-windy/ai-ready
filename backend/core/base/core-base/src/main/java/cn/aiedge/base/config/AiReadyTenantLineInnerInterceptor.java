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

    /** 租户不可解析时应跳过租户处理 */
    private boolean shouldSkip() {
        return MyBatisPlusConfig.getCurrentTenantIdValue() == null;
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
