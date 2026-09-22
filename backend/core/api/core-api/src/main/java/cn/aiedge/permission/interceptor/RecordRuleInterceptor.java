package cn.aiedge.permission.interceptor;

import cn.aiedge.permission.service.PermissionService;
import cn.aiedge.permission.service.RecordRuleService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 记录规则（{@code sys_record_rule}）的查询消费方。
 *
 * <p>本类把此前只有预览端点调用的 {@code RecordRuleService.buildDomainFilter} 真正接到查询链上：
 * 每条 SELECT 先按主表名找到该模型下的规则，把 Odoo 风格 domain 转成 JSQLParser
 * {@link Expression} 后作为额外的 AND 条件注入 WHERE，实现「规则自动过滤用户可见数据」。</p>
 *
 * <h3>为什么选「新增独立拦截器」而不选另外两条路</h3>
 * <ul>
 *   <li><b>(a) 扩展 {@code DataPermissionInterceptor}</b>：那条链是「注解 + 按表 data_scope」驱动，
 *       字段固定为 dept_id/create_by；记录规则是**任意字段 + 任意运算符**的 domain，还要按模型名去查
 *       另一张表。塞在一起会让一个已有 10 条单测兜底、逻辑已很密集的类同时承担两套口径，
 *       任何一处回归都会让「页面少单/多单」这种静默故障更难定位。</li>
 *   <li><b>(c) Service 层显式应用</b>：需要改动 erp/module 下每个查询方法，跨模块且必漏
 *       （漏掉一处就是静默越权），与「一处配置、全局生效」的记录规则定位相悖。</li>
 *   <li><b>(b) 新增独立拦截器</b>（选中）：与 {@code DataPermissionInterceptor} 同构，
 *       复用同一注册范式（{@code SmartInitializingSingleton} 插到分页插件之前），
 *       可单独测试、单独短路，互不影响。</li>
 * </ul>
 *
 * <h3>三条本仓血泪教训（务必遵守，改动前先读）</h3>
 * <ol>
 *   <li><b>MyBatis 拦截器自递归</b>：本类要查 {@code sys_record_rule}，而那条 SQL 会再走回本拦截器
 *       ⇒ 双重防护：① 实例级 {@link AtomicBoolean} 防重入，重入时直接返回；② 元数据表
 *       {@code sys_record_rule} 永久排除在过滤之外（见 {@link #SELF_METADATA_TABLE}）。
 *       {@code DataPermissionInterceptor} 已在 {@code sys_data_scope} 上踩过 StackOverflowError。</li>
 *   <li><b>{@code Map.of()} 不接受 null 值</b>：会抛 NPE，把「没有规则」这种**正常状态**变成 500。
 *       本类返回 {@code null} 表示「不注入」，绝不把可空值塞进 {@code Map.of()} 之类的不可空容器
 *       （对照 {@code RecordRuleController#buildDomain} 的 {@code Map.of("domain", domain)}，
 *       规则为空时 domain=null 即 500）。</li>
 *   <li><b>拦截器注册顺序</b>：多租户插件必须在分页插件之前，否则分页 count 漏租户条件；
 *       本拦截器同样必须插在 {@code PaginationInnerInterceptor} **之前**，否则 count 漏记录规则条件，
 *       出现「total 含无权记录、records 只有有权记录」的口径不一致。挂载见
 *       {@code RecordRuleInterceptorConfig}（顺序参照 {@code MyBatisPlusConfig} 的
 *       租户 → 分页 → 乐观锁）。</li>
 * </ol>
 *
 * <p><b>零行为变化</b>：{@code sys_record_rule} 当前 0 行 ⇒「启用模型集合」为空 ⇒ 本拦截器在
 * 解析 SQL、查会话**之前**就返回，不注入任何条件、不做任何解析。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.21
 */
@Slf4j
@RequiredArgsConstructor
public class RecordRuleInterceptor extends JsqlParserSupport implements InnerInterceptor {

    /** 记录规则自身的元数据表：永不参与过滤（防递归，见类注释第 1 条） */
    private static final String SELF_METADATA_TABLE = "sys_record_rule";

    private final RecordRuleService recordRuleService;
    private final PermissionService permissionService;

    /** 防重入闸：本拦截器在 beforeQuery 内触发的查询不得再次进入本拦截器 */
    private final AtomicBoolean processing = new AtomicBoolean(false);

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                           RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql)
            throws SQLException {

        // 防重入：加载规则/角色时触发的 SQL 会走到这里，直接放行（见类注释第 1 条）
        if (!processing.compareAndSet(false, true)) {
            return;
        }
        try {
            Expression expression = decideInjection(boundSql.getSql());
            if (expression == null) {
                return;
            }
            PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
            mpBs.sql(parserSingle(mpBs.sql(), expression));
        } catch (Exception e) {
            // 记录规则属安全策略：出错宁可本次不注入（不把请求打挂），但必须留痕，不能静默
            log.warn("记录规则查询过滤注入失败，本次查询不注入: {}", e.getMessage());
        } finally {
            processing.set(false);
        }
    }

    /**
     * 决定本次查询要注入的表达式；返回 {@code null} 表示不注入。
     *
     * <p>包私有：单元测试直接断言「规则为空时在查库之前短路」这一硬性验收点。</p>
     */
    Expression decideInjection(String sql) {
        // ① 零开销短路：表 0 行 / 无启用规则时，这里直接返回，绝不解析 SQL、不查会话
        Set<String> enabled = recordRuleService.getEnabledRecordRuleModels();
        if (enabled == null || enabled.isEmpty()) {
            return null;
        }

        String model = resolveModelName(sql);
        if (model == null || !enabled.contains(model)) {
            return null;
        }

        Long userId = permissionService.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        // 超管保持全局视野，与数据权限拦截器口径一致
        if (permissionService.isSuperAdmin()) {
            return null;
        }

        // 本仓「组」即角色（sys_record_rule.group_id ↔ sys_user_role.role_id）
        List<Long> groupIds = permissionService.getUserRoleIds(userId);
        String domain = recordRuleService.buildReadDomainFilter(model, userId, groupIds);
        return domainToExpression(domain);
    }

    /**
     * 从 SQL 解析出记录规则的模型名（本仓约定：主表名即 model_name）。
     *
     * <p>只支持**单表查询**：多表 JOIN 时裸列名可能歧义，宁可放弃过滤也不生成被误解的 SQL。
     * 非 SELECT、解析失败、FROM 子查询同样返回 {@code null}。元数据表自身返回 {@code null}（防递归）。</p>
     */
    static String resolveModelName(String sql) {
        String table = DataPermissionInterceptor.extractMainTable(sql);
        if (table == null) {
            return null;
        }
        if (SELF_METADATA_TABLE.equalsIgnoreCase(table)) {
            return null;
        }
        return table.toLowerCase();
    }

    /**
     * 把 Odoo 风格 domain 字符串转成 JSQLParser 表达式。
     *
     * <p>支持前缀运算符 {@code "|"}（或）、{@code "&"}（与）、{@code "!"}（非），
     * 条件三元组 {@code ["field","op",value]}，以及扁平条件列表（默认 OR，对齐 Odoo 语义）。</p>
     *
     * <p>返回 {@code null} 表示「无规则/不限」。domain 存在但解析不了时**不能放行**
     * （否则一条配错的规则 = 该表对所有人生效全量可见），故注入恒假条件
     * {@code null = null}（fail-closed，与数据权限的 noDataExpression 同口径）。</p>
     */
    static Expression domainToExpression(String domain) {
        if (StrUtil.isBlank(domain)) {
            return null;
        }
        try {
            return toExpression(JSONUtil.parseArray(domain));
        } catch (Exception e) {
            log.warn("记录规则 domain 解析失败，按「无数据」处理（fail-closed）: {}", domain);
            return noDataExpression();
        }
    }

    private static Expression toExpression(JSONArray domain) {
        if (domain == null || domain.isEmpty()) {
            return null;
        }
        Object first = domain.get(0);

        // 前缀运算符：["|", A, B] / ["&", A, B] / ["!", A]
        if (first instanceof String operator && isPrefixOperator(operator)) {
            if ("!".equals(operator)) {
                if (domain.size() < 2) {
                    return null;
                }
                Expression inner = toExpression(asArray(domain.get(1)));
                return inner == null ? null : new NotExpression(wrap(inner));
            }
            Expression combined = null;
            for (int i = 1; i < domain.size(); i++) {
                Expression part = toExpression(asArray(domain.get(i)));
                if (part == null) {
                    continue;
                }
                if (combined == null) {
                    combined = part;
                } else {
                    combined = "|".equals(operator)
                            ? new OrExpression(combined, part)
                            : new AndExpression(combined, part);
                }
            }
            return combined == null ? null : wrap(combined);
        }

        // 条件三元组：["field","op",value]
        if (first instanceof String) {
            return condition(domain);
        }

        // 扁平条件列表：默认 OR（Odoo 语义）
        Expression combined = null;
        for (Object item : domain) {
            Expression part = toExpression(asArray(item));
            if (part == null) {
                continue;
            }
            combined = combined == null ? part : new OrExpression(combined, part);
        }
        return combined == null ? null : wrap(combined);
    }

    private static Expression condition(JSONArray domain) {
        if (domain.size() < 2) {
            return null;
        }
        String field = domain.getStr(0);
        String operator = domain.getStr(1);
        Object value = domain.size() > 2 ? domain.get(2) : null;
        if (StrUtil.isBlank(field) || StrUtil.isBlank(operator)) {
            return null;
        }
        Column column = new Column(field.trim());

        switch (operator.trim().toLowerCase()) {
            case "=":
                return value == null ? isNull(column, false) : binary(new EqualsTo(), column, valueLiteral(value));
            case "!=":
            case "<>":
                return value == null ? isNull(column, true) : binary(new NotEqualsTo(), column, valueLiteral(value));
            case ">":
                return binary(new GreaterThan(), column, valueLiteral(value));
            case "<":
                return binary(new MinorThan(), column, valueLiteral(value));
            case ">=":
                return binary(new GreaterThanEquals(), column, valueLiteral(value));
            case "<=":
                return binary(new MinorThanEquals(), column, valueLiteral(value));
            case "like":
            case "ilike": {
                LikeExpression like = new LikeExpression();
                like.setLeftExpression(column);
                like.setRightExpression(new StringValue(String.valueOf(value)));
                like.setCaseInsensitive("ilike".equals(operator.trim().toLowerCase()));
                return like;
            }
            case "in":
            case "not in": {
                JSONArray values = JSONUtil.parseArray(value);
                List<Expression> items = new ArrayList<>();
                for (Object item : values) {
                    items.add(valueLiteral(item));
                }
                InExpression in = new InExpression();
                in.setLeftExpression(column);
                in.setRightExpression(new ParenthesedExpressionList<>(items));
                in.setNot("not in".equals(operator.trim().toLowerCase()));
                return in;
            }
            case "is null":
                return isNull(column, false);
            case "is not null":
                return isNull(column, true);
            default:
                // 未知运算符视为坏 domain：向上抛，由 domainToExpression 转成恒假条件（fail-closed）
                throw new IllegalArgumentException("不支持的记录规则运算符: " + operator);
        }
    }

    private static boolean isPrefixOperator(String token) {
        return "|".equals(token) || "&".equals(token) || "!".equals(token);
    }

    /** hutool 的嵌套数组元素是 JSONArray；标量原样包成单元素数组会被下面判为坏输入 */
    private static JSONArray asArray(Object value) {
        if (value instanceof JSONArray array) {
            return array;
        }
        throw new IllegalArgumentException("domain 子项不是数组: " + value);
    }

    private static Expression binary(BinaryExpression operator, Expression left, Expression right) {
        operator.setLeftExpression(left);
        operator.setRightExpression(right);
        return operator;
    }

    private static IsNullExpression isNull(Expression column, boolean not) {
        IsNullExpression isNull = new IsNullExpression();
        isNull.setLeftExpression(column);
        isNull.setNot(not);
        return isNull;
    }

    private static Expression valueLiteral(Object value) {
        if (value == null) {
            return new NullValue();
        }
        if (value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte) {
            return new LongValue(((Number) value).longValue());
        }
        if (value instanceof Number number) {
            return new DoubleValue(number.doubleValue());
        }
        return new StringValue(String.valueOf(value));
    }

    /** 加括号，保证 a OR b 作为整体被 AND 进原 WHERE 时不会因优先级被拆开 */
    private static Expression wrap(Expression expression) {
        return new ParenthesedExpressionList<>(expression);
    }

    /**
     * 「确实无权」的哨兵表达式：{@code WHERE null = null}（恒假，SQL 合法但查不出任何数据）。
     * 见 {@link #domainToExpression} 对 fail-closed 的说明。
     */
    private static Expression noDataExpression() {
        return new EqualsTo(new NullValue(), new NullValue());
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
     * 供单元测试断言「SQL 重写后长什么样」（写法照抄 {@code DataPermissionInterceptor#rewriteSqlForTest}）。
     *
     * <p>记录规则写错不会抛异常，只会静默漏数据/多数据，唯一验证手段就是断言重写后的 SQL 字符串。</p>
     */
    String rewriteSqlForTest(String sql, Expression whereExpression) {
        return parserSingle(sql, whereExpression);
    }
}
