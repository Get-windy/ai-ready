package cn.aiedge.permission.interceptor;

import cn.aiedge.permission.annotation.DataPermission;
import cn.aiedge.permission.service.PermissionService;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限拦截器
 * 基于 MyBatis-Plus InnerInterceptor 实现数据权限过滤
 * 使用 JSQLParser Expression 对象构建 WHERE 条件，避免 SQL 注入
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DataPermissionInterceptor extends JsqlParserSupport implements InnerInterceptor {

    private final PermissionService permissionService;

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                           RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
            throws SQLException {

        // 获取方法上的 DataPermission 注解
        String mapperId = ms.getId();
        DataPermission dataPermission = getDataPermissionAnnotation(mapperId);

        if (dataPermission == null) {
            return;
        }

        // 获取当前用户ID
        Long userId = permissionService.getCurrentUserId();
        if (userId == null) {
            return;
        }

        // 超级管理员跳过数据权限过滤
        if (permissionService.isSuperAdmin()) {
            return;
        }

        // 获取数据权限范围
        DataPermission.DataScopeType scopeType = dataPermission.scope();
        if (scopeType == DataPermission.DataScopeType.AUTO) {
            scopeType = getDataScopeFromUserRole();
        }

        // 使用 JSQLParser Expression 对象构建过滤条件（安全，无 SQL 注入）
        Expression whereExpression = buildWhereExpression(dataPermission.field(), scopeType, userId);
        if (whereExpression != null) {
            PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
            mpBs.sql(parserSingle(mpBs.sql(), whereExpression));
        }
    }

    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        if (!(obj instanceof Expression whereExpression)) {
            return;
        }
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        if (plainSelect.getWhere() == null) {
            plainSelect.setWhere(whereExpression);
        } else {
            plainSelect.setWhere(new AndExpression(plainSelect.getWhere(), whereExpression));
        }
    }

    /**
     * 获取 Mapper 方法上的 DataPermission 注解
     */
    private DataPermission getDataPermissionAnnotation(String mapperId) {
        try {
            String className = mapperId.substring(0, mapperId.lastIndexOf("."));
            String methodName = mapperId.substring(mapperId.lastIndexOf(".") + 1);
            // 去掉 MyBatis-Plus 动态方法后缀（如 "-lambda", "-plain"）
            int dashIdx = methodName.indexOf('-');
            if (dashIdx > 0) {
                methodName = methodName.substring(0, dashIdx);
            }

            Class<?> mapperClass = Class.forName(className);
            for (Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    return method.getAnnotation(DataPermission.class);
                }
            }
        } catch (Exception e) {
            log.debug("获取 DataPermission 注解失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据用户角色获取数据权限范围
     */
    private DataPermission.DataScopeType getDataScopeFromUserRole() {
        Integer dataScope = permissionService.getCurrentUserDataScope();
        if (dataScope == null) {
            return DataPermission.DataScopeType.ALL;
        }

        return switch (dataScope) {
            case 1 -> DataPermission.DataScopeType.DEPT;
            case 2 -> DataPermission.DataScopeType.DEPT_AND_CHILD;
            case 3 -> DataPermission.DataScopeType.SELF;
            default -> DataPermission.DataScopeType.ALL;
        };
    }

    /**
     * 构建 WHERE 条件表达式
     *
     * 使用 JSQLParser Expression 对象构建，而非字符串拼接，杜绝 SQL 注入。
     *
     * @param field     数据权限字段名（如 "create_by" 或 "dept_id"）
     * @param scopeType 数据权限范围
     * @param userId    当前用户ID
     * @return JSQLParser Expression，或 null（表示不添加过滤）
     */
    private Expression buildWhereExpression(String field, DataPermission.DataScopeType scopeType, Long userId) {
        return switch (scopeType) {
            case SELF -> {
                EqualsTo eq = new EqualsTo();
                eq.setLeftExpression(new Column(field));
                eq.setRightExpression(new LongValue(userId));
                yield eq;
            }
            case DEPT -> {
                Long deptId = permissionService.getCurrentUserDeptId();
                if (deptId == null) yield null;
                EqualsTo eq = new EqualsTo();
                eq.setLeftExpression(new Column("dept_id"));
                eq.setRightExpression(new LongValue(deptId));
                yield eq;
            }
            case DEPT_AND_CHILD -> {
                Set<Long> deptIds = permissionService.getCurrentUserDeptAndChildIds();
                if (deptIds == null || deptIds.isEmpty()) yield null;
                InExpression in = new InExpression();
                in.setLeftExpression(new Column("dept_id"));
                in.setRightExpression(new ParenthesedExpressionList<>(
                    deptIds.stream().map(LongValue::new).collect(Collectors.toList())
                ));
                yield in;
            }
            case ALL, AUTO -> null;
        };
    }
}
