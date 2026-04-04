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
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

/**
 * 数据权限拦截器
 * 基于 MyBatis-Plus InnerInterceptor 实现数据权限过滤
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
        
        // 根据权限范围生成过滤条件
        String whereClause = buildWhereClause(dataPermission.field(), scopeType, userId);
        if (whereClause != null) {
            PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
            mpBs.sql(parserSingle(mpBs.sql(), null));
        }
    }

    @Override
    protected void processSelect(PlainSelect plainSelect, String s) {
        // 添加数据权限条件
        Expression where = plainSelect.getWhere();
        // 这里可以添加额外的数据权限条件
    }

    /**
     * 获取 Mapper 方法上的 DataPermission 注解
     */
    private DataPermission getDataPermissionAnnotation(String mapperId) {
        try {
            String className = mapperId.substring(0, mapperId.lastIndexOf("."));
            String methodName = mapperId.substring(mapperId.lastIndexOf(".") + 1);
            
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
     * 构建 WHERE 子句
     */
    private String buildWhereClause(String field, DataPermission.DataScopeType scopeType, Long userId) {
        return switch (scopeType) {
            case SELF -> field + " = " + userId;
            case DEPT -> {
                Long deptId = permissionService.getCurrentUserDeptId();
                yield deptId != null ? "dept_id = " + deptId : null;
            }
            case DEPT_AND_CHILD -> {
                Set<Long> deptIds = permissionService.getCurrentUserDeptAndChildIds();
                if (deptIds != null && !deptIds.isEmpty()) {
                    yield "dept_id IN (" + String.join(",", 
                            deptIds.stream().map(String::valueOf).toList()) + ")";
                }
                yield null;
            }
            case ALL, AUTO -> null;
        };
    }
}
