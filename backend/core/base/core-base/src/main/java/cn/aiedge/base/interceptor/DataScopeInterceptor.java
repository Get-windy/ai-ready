package cn.aiedge.base.interceptor;

import cn.aiedge.base.context.DataScopeContextHolder;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;

/**
 * 数据权限拦截器（MyBatis-Plus InnerInterceptor）
 * 在 SELECT 语句执行时，读取 ThreadLocal 中的数据权限条件并注入到 SQL 中
 *
 * 注意：此拦截器需要添加到 MybatisPlusInterceptor 中，
 * 并且在 TenantLineInnerInterceptor 之后执行（租户条件先注入，数据权限条件后注入）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 检查是否启用了数据权限
        if (!DataScopeContextHolder.isDataScopeEnabled()) {
            return;
        }

        // 获取数据权限SQL条件
        String dataScopeSql = DataScopeContextHolder.getDataScopeSql();
        if (dataScopeSql == null || dataScopeSql.isEmpty()) {
            return;
        }

        // 获取原始SQL
        String originalSql = boundSql.getSql();

        try {
            // 注入数据权限条件
            String modifiedSql = injectDataScopeCondition(originalSql, dataScopeSql);
            if (modifiedSql != null && !modifiedSql.equals(originalSql)) {
                // 使用反射修改SQL
                setFieldValue(boundSql, "sql", modifiedSql);
                log.debug("数据权限SQL注入成功: {} -> {}", originalSql.substring(0, Math.min(50, originalSql.length())), dataScopeSql);
            }
        } catch (Exception e) {
            log.error("数据权限SQL注入失败: {}", e.getMessage());
        }
    }

    /**
     * 注入数据权限条件到SQL中
     */
    private String injectDataScopeCondition(String originalSql, String dataScopeSql) {
        try {
            // 解析SQL
            Statement statement = CCJSqlParserUtil.parse(originalSql);

            if (statement instanceof Select) {
                Select select = (Select) statement;

                // JSQLParser 4.x: Select.getBody() 返回 SelectBody (PlainSelect 或其他类型)
                if (select.getSelectBody() instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
                    Expression whereExpression = plainSelect.getWhere();

                    // 创建数据权限条件表达式
                    Expression dataScopeExpression = CCJSqlParserUtil.parseCondExpression(dataScopeSql);

                    if (whereExpression != null) {
                        // 与原有WHERE条件合并
                        AndExpression andExpression = new AndExpression(whereExpression, dataScopeExpression);
                        plainSelect.setWhere(andExpression);
                    } else {
                        // 直接设置为WHERE条件
                        plainSelect.setWhere(dataScopeExpression);
                    }

                    return select.toString();
                }
            }
        } catch (JSQLParserException e) {
            // 解析失败时使用简单字符串拼接方式
            log.warn("JSQLParser解析失败，使用简单拼接: {}", e.getMessage());
            return appendDataScopeConditionSimple(originalSql, dataScopeSql);
        }

        return originalSql;
    }

    /**
     * 简单字符串拼接方式注入数据权限条件
     */
    private String appendDataScopeConditionSimple(String originalSql, String dataScopeSql) {
        // 去除末尾分号
        String sql = originalSql.trim();
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }

        // 判断是否已有 WHERE 条件
        if (sql.toUpperCase().contains("WHERE")) {
            // 在 WHERE 后追加条件
            int whereIndex = sql.toUpperCase().indexOf("WHERE");
            String beforeWhere = sql.substring(0, whereIndex + 5);
            String afterWhere = sql.substring(whereIndex + 5);

            // 查找 GROUP BY / ORDER BY / LIMIT 等位置
            int groupByIndex = afterWhere.toUpperCase().indexOf("GROUP BY");
            int orderByIndex = afterWhere.toUpperCase().indexOf("ORDER BY");
            int limitIndex = afterWhere.toUpperCase().indexOf("LIMIT");

            int insertIndex = findMinPositiveIndex(groupByIndex, orderByIndex, limitIndex, afterWhere.length());

            String conditionsPart = afterWhere.substring(0, insertIndex);
            String tailPart = afterWhere.substring(insertIndex);

            return beforeWhere + conditionsPart + " AND " + dataScopeSql + " " + tailPart;
        } else {
            // 无 WHERE 条件，直接添加
            int groupByIndex = sql.toUpperCase().indexOf("GROUP BY");
            int orderByIndex = sql.toUpperCase().indexOf("ORDER BY");
            int limitIndex = sql.toUpperCase().indexOf("LIMIT");

            int insertIndex = findMinPositiveIndex(groupByIndex, orderByIndex, limitIndex, sql.length());

            String beforePart = sql.substring(0, insertIndex);
            String afterPart = sql.substring(insertIndex);

            return beforePart + " WHERE " + dataScopeSql + " " + afterPart;
        }
    }

    /**
     * 找到最小正索引
     */
    private int findMinPositiveIndex(int... indexes) {
        int minIndex = Integer.MAX_VALUE;
        for (int index : indexes) {
            if (index > 0 && index < minIndex) {
                minIndex = index;
            }
        }
        return minIndex == Integer.MAX_VALUE ? 0 : minIndex;
    }

    /**
     * 使用反射设置字段值
     */
    private void setFieldValue(Object object, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}