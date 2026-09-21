package cn.aiedge.permission.interceptor;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据权限的 SQL 重写测试。
 *
 * <p><b>为什么必须有它</b>：数据权限写错了既不会抛异常、也不会报错，只会**静默地**漏数据或多数据
 * ——「页面少了几条单」和「不该看的人看到了」都不会有任何信号。
 * 唯一能证明它按预期工作（或按预期放弃）的手段，就是断言重写后的 SQL 字符串。
 * 这是从隔壁 yudao 系项目学到的一条（他们用 {@code assertSql(sql, expected)} 覆盖了
 * JOIN / 子查询 / WITH AS 大量边界）。</p>
 *
 * <p>本测试不启动 Spring、不连数据库，秒级完成。</p>
 *
 * @author AI-Ready Team
 * @since 0.3.21
 */
class DataPermissionSqlRewriteTest {

    // ══════════════ ① 主表解析：决定「这条 SQL 归不归数据权限管」 ══════════════

    @Test
    @DisplayName("单表查询：解析出主表名")
    void parsesSingleTable() {
        assertThat(DataPermissionInterceptor.extractMainTable(
                "SELECT id, name FROM erp_sale_order WHERE status = 1"))
                .isEqualTo("erp_sale_order");
    }

    @Test
    @DisplayName("带别名的单表查询：仍解析出真实表名")
    void parsesSingleTableWithAlias() {
        assertThat(DataPermissionInterceptor.extractMainTable(
                "SELECT o.id FROM erp_sale_order o WHERE o.status = 1"))
                .isEqualTo("erp_sale_order");
    }

    @Test
    @DisplayName("多表 JOIN：主动放弃（裸列名 dept_id 会歧义，宁可不注入）")
    void givesUpOnJoin() {
        assertThat(DataPermissionInterceptor.extractMainTable(
                "SELECT o.id FROM erp_sale_order o LEFT JOIN erp_customer c ON c.id = o.customer_id"))
                .as("显式 JOIN 必须放弃")
                .isNull();

        assertThat(DataPermissionInterceptor.extractMainTable(
                "SELECT o.id FROM erp_sale_order o, erp_customer c WHERE c.id = o.customer_id"))
                .as("隐式内连接（逗号）同样必须放弃")
                .isNull();
    }

    @Test
    @DisplayName("FROM 是子查询：解析不出主表，放弃（不误注入）")
    void givesUpOnSubQueryFrom() {
        assertThat(DataPermissionInterceptor.extractMainTable(
                "SELECT t.id FROM (SELECT id FROM erp_sale_order) t"))
                .isNull();
    }

    @Test
    @DisplayName("非 SELECT 语句：不处理")
    void ignoresNonSelect() {
        assertThat(DataPermissionInterceptor.extractMainTable(
                "UPDATE erp_sale_order SET status = 2")).isNull();
        assertThat(DataPermissionInterceptor.extractMainTable(
                "DELETE FROM erp_sale_order")).isNull();
        assertThat(DataPermissionInterceptor.extractMainTable(
                "INSERT INTO erp_sale_order (id) VALUES (1)")).isNull();
    }

    @Test
    @DisplayName("解析失败：不抛异常，返回 null（不能因为一条怪 SQL 就把请求打挂）")
    void toleratesBrokenSql() {
        assertThat(DataPermissionInterceptor.extractMainTable("this is not sql at all")).isNull();
        assertThat(DataPermissionInterceptor.extractMainTable("")).isNull();
        assertThat(DataPermissionInterceptor.extractMainTable("SELECT * FROM")).isNull();
    }

    // ══════════════ ② 注入效果：重写后的 SQL 必须真的带上条件 ══════════════

    @Test
    @DisplayName("原本没有 WHERE：注入后新增 WHERE dept_id = 1")
    void injectsWhereWhenAbsent() {
        String rewritten = rewrite("SELECT * FROM erp_sale_order", deptEquals(1L));

        assertThat(rewritten).containsIgnoringCase("dept_id = 1");
        assertThat(rewritten).containsIgnoringCase("erp_sale_order");
    }

    @Test
    @DisplayName("原本有 WHERE：以 AND 追加，原条件不丢（防把用户条件覆盖掉）")
    void appendsWithAnd() {
        String rewritten = rewrite("SELECT * FROM erp_sale_order WHERE status = 1", deptEquals(1L));

        assertThat(rewritten).containsIgnoringCase("status = 1");
        assertThat(rewritten).containsIgnoringCase("dept_id = 1");
        assertThat(rewritten).containsIgnoringCase("AND");
    }

    @Test
    @DisplayName("无权限哨兵：注入 null = null（恒假）——SQL 合法但查不出任何数据")
    void noDataSentinelIsInjected() {
        // 这正是「用户不属于任何部门」「该表没配规则」时应该走的分支：
        // 不能返回 null（那等于不加过滤 = 全量可见，fail-open）
        EqualsTo sentinel = new EqualsTo(new NullValue(), new NullValue());
        String rewritten = rewrite("SELECT * FROM erp_sale_order WHERE status = 1", sentinel);

        assertThat(rewritten).containsIgnoringCase("status = 1");
        assertThat(rewritten).containsIgnoringCase("null = null");
    }

    @Test
    @DisplayName("带 GROUP BY / ORDER BY 的查询：条件插在正确位置（不破坏语句结构）")
    void injectsBeforeGroupBy() {
        String rewritten = rewrite(
                "SELECT dept_id, COUNT(*) FROM erp_sale_order WHERE status = 1 GROUP BY dept_id ORDER BY dept_id",
                deptEquals(7L));

        assertThat(rewritten).containsIgnoringCase("dept_id = 7");
        assertThat(rewritten).containsIgnoringCase("GROUP BY");
        assertThat(rewritten).containsIgnoringCase("ORDER BY");
        // 条件必须在 GROUP BY 之前，否则 SQL 语法错误
        assertThat(rewritten.toUpperCase().indexOf("DEPT_ID = 7"))
                .isLessThan(rewritten.toUpperCase().indexOf("GROUP BY"));
    }

    // ══════════════ 辅助 ══════════════

    /** PermissionService 传 null：本测试只碰 SQL 重写（beforeQuery 才会用到它） */
    private String rewrite(String sql, Expression expression) {
        return new DataPermissionInterceptor(null).rewriteSqlForTest(sql, expression);
    }

    private EqualsTo deptEquals(long deptId) {
        EqualsTo eq = new EqualsTo();
        eq.setLeftExpression(new Column("dept_id"));
        eq.setRightExpression(new LongValue(deptId));
        return eq;
    }
}
