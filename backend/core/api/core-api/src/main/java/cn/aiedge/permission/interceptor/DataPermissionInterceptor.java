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
import net.sf.jsqlparser.expression.NullValue;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    /** 数据权限自身的元数据表：永不参与过滤（防递归，见 resolveDataScopeTable） */
    private static final String SELF_METADATA_TABLE = "sys_data_scope";

    private final PermissionService permissionService;

    /**
     * Mapper 方法注解查找结果缓存。
     *
     * 为什么必须缓存：本拦截器挂在 MyBatis 插件链上，**每一条 SQL 都会走 beforeQuery**，
     * 而注解查找要 `Class.forName` + 反射遍历方法；绝大多数 Mapper 方法并没有 @DataPermission，
     * 不缓存等于给每次查询都加一次反射扫描。
     * 用 Optional 包装是因为 ConcurrentHashMap 不接受 null 值（无注解也是一种要缓存的结果）。
     */
    private final Map<String, Optional<DataPermission>> annotationCache = new ConcurrentHashMap<>();

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                           RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
            throws SQLException {

        String sql = boundSql.getSql();
        DataPermission dataPermission = getDataPermissionAnnotation(ms.getId());

        // 表级自动模式（对标用友「数据权限控制设置」）：
        // 没有注解时，看管理员是否在「角色 → 数据范围」里为**本表**配置过规则。
        // 未配置任何表时 enabled 集合为空 → 立即返回，这是绝大多数查询的路径，零额外开销。
        String mainTable;
        if (dataPermission == null) {
            mainTable = resolveDataScopeTable(sql);
            if (mainTable == null) {
                return;
            }
        } else {
            mainTable = extractMainTable(sql);
        }

        // 获取当前用户ID（放在廉价判断之后，避免每条 SQL 都做会话/角色查询）
        Long userId = permissionService.getCurrentUserId();
        if (userId == null) {
            return;
        }

        // 超级管理员跳过数据权限过滤
        if (permissionService.isSuperAdmin()) {
            return;
        }

        // 获取数据权限范围：注解优先，其次按角色 data_scope（表级模式统一走自定义规则）
        DataPermission.DataScopeType scopeType;
        String field;
        if (dataPermission != null) {
            scopeType = dataPermission.scope();
            field = dataPermission.field();
            if (scopeType == DataPermission.DataScopeType.AUTO) {
                scopeType = getDataScopeFromUserRole();
            }
        } else {
            scopeType = DataPermission.DataScopeType.CUSTOM;
            field = "dept_id";
        }

        // 使用 JSQLParser Expression 对象构建过滤条件（安全，无 SQL 注入）
        Expression whereExpression = buildWhereExpression(field, scopeType, userId, mainTable);
        if (whereExpression != null) {
            PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
            mpBs.sql(parserSingle(mpBs.sql(), whereExpression));
        }
    }

    /**
     * 表级自动模式判定：当前 SQL 的主表是否在「已启用数据权限控制」的表清单里。
     *
     * <p>先用 {@code contains} 做廉价粗筛（启用表通常个位数），命中才做 JSQLParser 解析，
     * 避免给每条 SQL 都加上解析成本。</p>
     *
     * @return 命中时返回主表名，否则 null
     */
    private String resolveDataScopeTable(String sql) {
        java.util.Set<String> enabled = permissionService.getEnabledDataScopeTables();
        if (enabled.isEmpty()) {
            return null;
        }
        String lowerSql = sql.toLowerCase();
        boolean maybe = false;
        for (String table : enabled) {
            if (lowerSql.contains(table)) {
                maybe = true;
                break;
            }
        }
        if (!maybe) {
            return null;
        }
        String mainTable = extractMainTable(sql);
        if (mainTable == null) {
            return null;
        }
        // 数据权限自身的元数据表永不参与过滤：拦截器读它时若再注入条件会形成递归调用
        // （sys_data_scope 被误配进「数据范围」就会触发，属配置错误但必须兜住）
        if (SELF_METADATA_TABLE.equalsIgnoreCase(mainTable)) {
            return null;
        }
        return enabled.contains(mainTable.toLowerCase()) ? mainTable : null;
    }

    /**
     * 解析 SQL 的主表名。
     *
     * <p>⚠️ 只支持**单表查询**：多表 JOIN 时注入的裸列名（如 {@code dept_id}）可能歧义，
     * 宁可放弃过滤也不生成有歧义或被误解的 SQL。</p>
     *
     * @return 主表名；解析失败或多表 JOIN 时返回 null
     */
    // 包私有 static：不依赖实例状态，且需要被同包单元测试直接断言（数据权限写错不会抛异常，
    // 只会静默漏数据/多数据，所以这段解析逻辑必须有测试兜着）。
    static String extractMainTable(String sql) {
        try {
            net.sf.jsqlparser.statement.Statement statement =
                    net.sf.jsqlparser.parser.CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof net.sf.jsqlparser.statement.select.Select select)) {
                return null;
            }
            if (!(select.getSelectBody() instanceof PlainSelect plainSelect)) {
                return null;
            }
            if (plainSelect.getJoins() != null && !plainSelect.getJoins().isEmpty()) {
                return null;
            }
            if (plainSelect.getFromItem() instanceof net.sf.jsqlparser.schema.Table table) {
                return table.getName();
            }
        } catch (Exception e) {
            log.debug("解析 SQL 主表失败: {}", e.getMessage());
        }
        return null;
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
     * 供单元测试断言「SQL 重写后长什么样」。
     *
     * <p>数据权限写错**不会抛异常** —— 只会静默漏数据或多数据，所以唯一的验证手段就是
     * 断言重写后的 SQL 字符串（隔壁 yudao 系项目的 DataPermissionRuleHandlerTest 同样是这个套路）。
     * {@code parserSingle} 是父类（MyBatis-Plus 的 JsqlParserSupport）的 protected 方法，
     * 测试类既不在其子类链上、也不同包，故在此开一个包私有口子。</p>
     */
    String rewriteSqlForTest(String sql, Expression whereExpression) {
        return parserSingle(sql, whereExpression);
    }

    /**
     * 获取 Mapper 方法上的 DataPermission 注解（带缓存，见 annotationCache 注释）
     */
    private DataPermission getDataPermissionAnnotation(String mapperId) {
        return annotationCache
                .computeIfAbsent(mapperId, id -> Optional.ofNullable(lookupDataPermissionAnnotation(id)))
                .orElse(null);
    }

    /**
     * 反射查找 Mapper 方法上的 DataPermission 注解（仅在缓存未命中时执行）
     */
    private DataPermission lookupDataPermissionAnnotation(String mapperId) {
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
            case 4 -> DataPermission.DataScopeType.CUSTOM;
            default -> DataPermission.DataScopeType.ALL;
        };
    }

    /**
     * 「确实无权」的哨兵表达式：{@code WHERE null = null}（恒假，SQL 合法但查不出任何数据）。
     *
     * <p><b>为什么不能返回 null</b>：null 在调用方意味着「不添加过滤」= **可见全部数据**，
     * 于是「用户不属于任何部门」这种本该什么都看不到的情况会变成**越权放行**（fail-open）。
     * 三态划分参照隔壁 yudao 系项目的 {@code DeptDataPermissionRule}：
     * 全部可见 → 不注入；确实无权 → 恒假条件；配置缺失 → 恒假条件 + 告警。</p>
     */
    private static Expression noDataExpression() {
        // 每次新建而非共享常量：Expression 对象要参与 SQL 重写，避免同一实例被复用修改
        return new EqualsTo(new NullValue(), new NullValue());
    }

    /**
     * 构建 WHERE 条件表达式
     *
     * 使用 JSQLParser Expression 对象构建，而非字符串拼接，杜绝 SQL 注入。
     *
     * @param field     数据权限字段名（如 "create_by" 或 "dept_id"）
     * @param scopeType 数据权限范围
     * @param userId    当前用户ID
     * @param tableName 当前 SQL 的主表名（CUSTOM 规则按表配置，必须用它去查，不能用字段名）
     * @return JSQLParser Expression，或 null（表示不添加过滤）
     */
    private Expression buildWhereExpression(String field, DataPermission.DataScopeType scopeType, Long userId,
                                           String tableName) {
        return switch (scopeType) {
            case SELF -> {
                EqualsTo eq = new EqualsTo();
                eq.setLeftExpression(new Column(field));
                eq.setRightExpression(new LongValue(userId));
                yield eq;
            }
            case DEPT -> {
                Long deptId = permissionService.getCurrentUserDeptId();
                // 无部门 ≠ 可见全部：注入恒假条件（见 noDataExpression 注释）
                if (deptId == null) yield noDataExpression();
                EqualsTo eq = new EqualsTo();
                eq.setLeftExpression(new Column("dept_id"));
                eq.setRightExpression(new LongValue(deptId));
                yield eq;
            }
            case DEPT_AND_CHILD -> {
                Set<Long> deptIds = permissionService.getCurrentUserDeptAndChildIds();
                if (deptIds == null || deptIds.isEmpty()) yield noDataExpression();
                InExpression in = new InExpression();
                in.setLeftExpression(new Column("dept_id"));
                in.setRightExpression(new ParenthesedExpressionList<>(
                    deptIds.stream().map(LongValue::new).collect(Collectors.toList())
                ));
                yield in;
            }
            case ALL, AUTO -> null;
            case CUSTOM -> {
                // ⚠️ 这里必须传**表名**：getUserCustomDataScopes 的语义是「按表查该用户角色的规则」，
                //    此前传的是 field（字段名，如 dept_id）→ 按表名匹配永远查不到，CUSTOM 分支形同虚设
                //    （2026-09-20 修复）。表名解析不出（多表 JOIN）时退回按字段名查，保持旧行为。
                java.util.List<cn.aiedge.base.entity.SysDataScope> customScopes =
                    permissionService.getUserCustomDataScopes(tableName != null ? tableName : field);
                if (customScopes == null || customScopes.isEmpty()) {
                    // 「该表没有为当前用户角色配置任何规则」属**配置缺失**，同样不能放行：
                    // 否则漏配一张表 = 该表对所有角色全量可见（静默越权，且没有任何信号）
                    log.warn("数据权限：表 {} 未为当前用户角色配置任何规则，按「无数据」处理"
                            + "（若确实要放行，请显式配置一条 ruleType=ALL 的规则）", tableName);
                    yield noDataExpression();
                }

                // 合并所有自定义规则的 WHERE 条件
                net.sf.jsqlparser.expression.Expression combined = null;
                for (cn.aiedge.base.entity.SysDataScope scope : customScopes) {
                    Expression customExpr = buildCustomExpression(scope, userId);
                    if (customExpr != null) {
                        if (combined == null) {
                            combined = customExpr;
                        } else {
                            combined = new net.sf.jsqlparser.expression.operators.conditional.OrExpression(combined, customExpr);
                        }
                    }
                }
                yield combined;
            }
        };
    }

    /**
     * 根据自定义数据权限规则构建表达式
     */
    private Expression buildCustomExpression(cn.aiedge.base.entity.SysDataScope scope, Long userId) {
        if (scope == null) return null;
        String ruleType = scope.getRuleType();
        if (ruleType == null) return null;

        return switch (ruleType) {
            case "ALL" -> null;
            case "SELF" -> {
                EqualsTo eq = new EqualsTo();
                eq.setLeftExpression(new Column(scope.getTargetField() != null ? scope.getTargetField() : "create_by"));
                eq.setRightExpression(new LongValue(userId));
                yield eq;
            }
            case "DEPT" -> {
                String deptIds = scope.getDeptIds();
                if (deptIds == null || deptIds.isBlank()) yield null;
                try {
                    java.util.List<Object> ids = cn.hutool.json.JSONUtil.parseArray(deptIds);
                    if (ids.isEmpty()) yield null;
                    String field = scope.getTargetField() != null ? scope.getTargetField() : "dept_id";
                    if (ids.size() == 1) {
                        EqualsTo eq = new EqualsTo();
                        eq.setLeftExpression(new Column(field));
                        eq.setRightExpression(new LongValue(Long.valueOf(ids.get(0).toString())));
                        yield eq;
                    } else {
                        InExpression in = new InExpression();
                        in.setLeftExpression(new Column(field));
                        in.setRightExpression(new net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList<>(
                            ids.stream().map(id -> new LongValue(Long.valueOf(id.toString()))).collect(java.util.stream.Collectors.toList())
                        ));
                        yield in;
                    }
                } catch (Exception e) {
                    log.warn("解析自定义数据权限部门ID失败: {}", e.getMessage());
                    yield null;
                }
            }
            case "DEPT_AND_CHILD" -> {
                // 同 DEPT 逻辑；字段名取规则自身配置的 targetField（此前硬编码 dept_id，
                // 与 DEPT 分支不一致 —— 管理员把 targetField 配成别的字段时会用错列）
                String deptIds = scope.getDeptIds();
                if (deptIds == null || deptIds.isBlank()) yield null;
                try {
                    java.util.List<Object> ids = cn.hutool.json.JSONUtil.parseArray(deptIds);
                    if (ids.isEmpty()) yield null;
                    String field = scope.getTargetField() != null ? scope.getTargetField() : "dept_id";
                    if (ids.size() == 1) {
                        EqualsTo eq = new EqualsTo();
                        eq.setLeftExpression(new Column(field));
                        eq.setRightExpression(new LongValue(Long.valueOf(ids.get(0).toString())));
                        yield eq;
                    } else {
                        InExpression in = new InExpression();
                        in.setLeftExpression(new Column(field));
                        in.setRightExpression(new net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList<>(
                            ids.stream().map(id -> new LongValue(Long.valueOf(id.toString()))).collect(java.util.stream.Collectors.toList())
                        ));
                        yield in;
                    }
                } catch (Exception e) {
                    log.warn("解析自定义数据权限部门ID失败: {}", e.getMessage());
                    yield null;
                }
            }
            case "CUSTOM_SQL" -> {
                String customSql = scope.getCustomSql();
                if (customSql == null || customSql.isBlank()) yield null;
                try {
                    // 使用 JSQLParser 解析自定义 SQL 表达式（安全，非字符串拼接）
                    net.sf.jsqlparser.expression.Expression parsedExpr =
                        net.sf.jsqlparser.parser.CCJSqlParserUtil.parseExpression(customSql);
                    yield parsedExpr;
                } catch (Exception e) {
                    log.warn("解析自定义SQL表达式失败, 回退到字符串拼接: {}", e.getMessage());
                    // 兜底：只有已知安全的简单条件才允许字符串拼接
                    if (customSql.matches("^[a-zA-Z_]+\\s*(=|!=|<|>|<=|>=|IN|NOT IN|LIKE)\\s*[a-zA-Z0-9_'(), ]+$")) {
                        yield new net.sf.jsqlparser.expression.operators.conditional.AndExpression(
                            new net.sf.jsqlparser.expression.StringValue(" " + customSql + " "),
                            new net.sf.jsqlparser.expression.LongValue(1)
                        );
                    }
                    yield null;
                }
            }
            default -> null;
        };
    }
}
