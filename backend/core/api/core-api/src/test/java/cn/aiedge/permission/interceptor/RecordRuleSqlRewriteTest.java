package cn.aiedge.permission.interceptor;

import cn.aiedge.permission.service.PermissionService;
import cn.aiedge.permission.service.RecordRuleService;
import net.sf.jsqlparser.expression.Expression;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 记录规则的 SQL 重写测试。
 *
 * <p><b>为什么必须有它</b>：记录规则写错了既不会抛异常、也不会报错，只会**静默地**漏数据或多数据。
 * 唯一能证明它按预期工作（或按预期放弃）的手段，就是断言重写后的 SQL 字符串 ——
 * 与 {@link DataPermissionSqlRewriteTest} 同一套路。</p>
 *
 * <p>本测试不启动 Spring、不连数据库、不跑 MyBatis 链，秒级完成。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.21
 */
class RecordRuleSqlRewriteTest {

    // ══════════════ ① 零行为变化：规则为空时必须在查库之前短路 ══════════════

    @Test
    @DisplayName("规则为空（表 0 行）：不注入任何条件，且不查规则、不碰会话")
    void doesNothingWhenNoRules() {
        RecordRuleService ruleService = mock(RecordRuleService.class);
        PermissionService permissionService = mock(PermissionService.class);
        when(ruleService.getEnabledRecordRuleModels()).thenReturn(Set.of());

        RecordRuleInterceptor interceptor = new RecordRuleInterceptor(ruleService, permissionService);

        assertThat(interceptor.decideInjection("SELECT * FROM erp_sale_order")).isNull();
        // 判空必须在「查库」之前：规则查询根本没被调用
        verify(ruleService, never()).buildReadDomainFilter(any(), any(), any());
        // 连会话/角色都没查 —— 这才是「零开销」
        verifyNoInteractions(permissionService);
    }

    @Test
    @DisplayName("有启用规则但本表未配置：不注入")
    void doesNotInjectForUnconfiguredModel() {
        RecordRuleService ruleService = mock(RecordRuleService.class);
        PermissionService permissionService = mock(PermissionService.class);
        when(ruleService.getEnabledRecordRuleModels()).thenReturn(Set.of("erp_sale_order"));

        RecordRuleInterceptor interceptor = new RecordRuleInterceptor(ruleService, permissionService);

        assertThat(interceptor.decideInjection("SELECT * FROM erp_customer")).isNull();
        verify(ruleService, never()).buildReadDomainFilter(any(), any(), any());
    }

    // ══════════════ ② 命中：单表 → 真的把条件注入进去 ══════════════

    @Test
    @DisplayName("单表命中：按主表名找到规则并注入读权限 domain")
    void injectsDomainForMatchedTable() {
        RecordRuleService ruleService = mock(RecordRuleService.class);
        PermissionService permissionService = mock(PermissionService.class);
        when(ruleService.getEnabledRecordRuleModels()).thenReturn(Set.of("erp_sale_order"));
        when(permissionService.getCurrentUserId()).thenReturn(9L);
        when(permissionService.isSuperAdmin()).thenReturn(false);
        when(permissionService.getUserRoleIds(9L)).thenReturn(List.of());
        when(ruleService.buildReadDomainFilter("erp_sale_order", 9L, List.of()))
                .thenReturn("[\"create_by\",\"=\",9]");

        RecordRuleInterceptor interceptor = new RecordRuleInterceptor(ruleService, permissionService);
        Expression expression = interceptor.decideInjection("SELECT * FROM erp_sale_order");

        assertThat(expression).isNotNull();
        String rewritten = interceptor.rewriteSqlForTest("SELECT * FROM erp_sale_order", expression);
        assertThat(rewritten).containsIgnoringCase("create_by = 9");
    }

    // ══════════════ ③ 模型解析：决定「这条 SQL 归不归记录规则管」 ══════════════

    @Test
    @DisplayName("单表查询：解析出模型名（主表名）")
    void parsesSingleTable() {
        assertThat(RecordRuleInterceptor.resolveModelName(
                "SELECT id, name FROM erp_sale_order WHERE status = 1"))
                .isEqualTo("erp_sale_order");
    }

    @Test
    @DisplayName("带别名的单表查询：仍解析出真实表名")
    void parsesSingleTableWithAlias() {
        assertThat(RecordRuleInterceptor.resolveModelName(
                "SELECT o.id FROM erp_sale_order o WHERE o.status = 1"))
                .isEqualTo("erp_sale_order");
    }

    @Test
    @DisplayName("多表 JOIN：主动放弃（裸列名会歧义，宁可不注入）")
    void givesUpOnJoin() {
        assertThat(RecordRuleInterceptor.resolveModelName(
                "SELECT o.id FROM erp_sale_order o LEFT JOIN erp_customer c ON c.id = o.customer_id"))
                .as("显式 JOIN 必须放弃")
                .isNull();
        assertThat(RecordRuleInterceptor.resolveModelName(
                "SELECT o.id FROM erp_sale_order o, erp_customer c WHERE c.id = o.customer_id"))
                .as("隐式内连接（逗号）同样必须放弃")
                .isNull();
    }

    @Test
    @DisplayName("非 SELECT 语句：不处理")
    void ignoresNonSelect() {
        assertThat(RecordRuleInterceptor.resolveModelName("UPDATE erp_sale_order SET status = 2")).isNull();
        assertThat(RecordRuleInterceptor.resolveModelName("DELETE FROM erp_sale_order")).isNull();
        assertThat(RecordRuleInterceptor.resolveModelName("INSERT INTO erp_sale_order (id) VALUES (1)")).isNull();
    }

    @Test
    @DisplayName("规则元数据表自身：不处理（防拦截器自递归）")
    void ignoresSelfMetadataTable() {
        assertThat(RecordRuleInterceptor.resolveModelName("SELECT * FROM sys_record_rule")).isNull();
    }

    // ══════════════ ④ domain → SQL：坏输入 fail-closed，不静默放行 ══════════════

    @Test
    @DisplayName("坏 domain：不抛异常，注入恒假条件 null = null（fail-closed）")
    void toleratesBrokenDomain() {
        Expression expression = RecordRuleInterceptor.domainToExpression("这不是一个 domain");
        assertThat(expression).isNotNull();

        String rewritten = new RecordRuleInterceptor(null, null)
                .rewriteSqlForTest("SELECT * FROM erp_sale_order WHERE status = 1", expression);
        assertThat(rewritten).containsIgnoringCase("status = 1");
        assertThat(rewritten).containsIgnoringCase("null = null");
    }

    @Test
    @DisplayName("未知运算符：同样 fail-closed（不能当作「无限制」放行）")
    void unknownOperatorIsFailClosed() {
        Expression expression = RecordRuleInterceptor.domainToExpression("[\"status\",\"~=\",1]");
        assertThat(expression).isNotNull();
        assertThat(expression.toString()).containsIgnoringCase("null = null");
    }

    @Test
    @DisplayName("空 domain：表示不限制，返回 null（不注入）")
    void blankDomainMeansNoRestriction() {
        assertThat(RecordRuleInterceptor.domainToExpression(null)).isNull();
        assertThat(RecordRuleInterceptor.domainToExpression("")).isNull();
        assertThat(RecordRuleInterceptor.domainToExpression("[]")).isNull();
    }

    // ══════════════ ⑤ 注入位置：结构必须合法 ══════════════

    @Test
    @DisplayName("原本没有 WHERE：注入后新增条件")
    void injectsWhereWhenAbsent() {
        String rewritten = rewrite("SELECT * FROM erp_sale_order", "[\"create_by\",\"=\",1]");

        assertThat(rewritten).containsIgnoringCase("create_by = 1");
        assertThat(rewritten).containsIgnoringCase("erp_sale_order");
    }

    @Test
    @DisplayName("原本有 WHERE：以 AND 追加，原条件不丢（防把用户条件覆盖掉）")
    void appendsWithAnd() {
        String rewritten = rewrite("SELECT * FROM erp_sale_order WHERE status = 1",
                "[\"create_by\",\"=\",1]");

        assertThat(rewritten).containsIgnoringCase("status = 1");
        assertThat(rewritten).containsIgnoringCase("create_by = 1");
        assertThat(rewritten).containsIgnoringCase("AND");
    }

    @Test
    @DisplayName("带 GROUP BY 的查询：条件插在 GROUP BY 之前（不破坏语句结构）")
    void injectsBeforeGroupBy() {
        String rewritten = rewrite(
                "SELECT create_by, COUNT(*) FROM erp_sale_order WHERE status = 1 GROUP BY create_by ORDER BY create_by",
                "[\"create_by\",\"=\",7]");

        assertThat(rewritten).containsIgnoringCase("create_by = 7");
        assertThat(rewritten).containsIgnoringCase("GROUP BY");
        assertThat(rewritten).containsIgnoringCase("ORDER BY");
        // 条件必须在 GROUP BY 之前，否则 SQL 语法错误
        assertThat(rewritten.toUpperCase().indexOf("CREATE_BY = 7"))
                .isLessThan(rewritten.toUpperCase().indexOf("GROUP BY"));
    }

    @Test
    @DisplayName("或运算 domain：整体加括号，AND 进原 WHERE 时优先级不被拆开")
    void wrapsOrDomain() {
        String rewritten = rewrite("SELECT * FROM erp_sale_order WHERE status = 1",
                "[\"|\", [\"create_by\",\"=\",1], [\"create_by\",\"=\",2]]");

        assertThat(rewritten).containsIgnoringCase("status = 1");
        assertThat(rewritten).containsIgnoringCase("create_by = 1");
        assertThat(rewritten).containsIgnoringCase("create_by = 2");
        assertThat(rewritten).containsIgnoringCase("OR");
        // 括号必须包住整个 OR，否则 status = 1 AND a = 1 OR a = 2 语义错误
        assertThat(rewritten).contains("(");
    }

    // ══════════════ 辅助 ══════════════

    private String rewrite(String sql, String domain) {
        Expression expression = RecordRuleInterceptor.domainToExpression(domain);
        assertThat(expression).as("domain 应能转成表达式: %s", domain).isNotNull();
        return new RecordRuleInterceptor(null, null).rewriteSqlForTest(sql, expression);
    }
}
