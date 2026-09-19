package cn.aiedge.datasource.support;

import cn.aiedge.datasource.config.DataMaintenanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 清理规则的输入安全闸门（SQL 注入面的唯一入口防线）
 *
 * <p>🔴 为什么必须有这一层：{@code sys_data_cleanup_rule.target_table} / {@code condition_column}
 * 是**自由文本**列（前端就是一个输入框）。如果不加校验就拼进 {@code DELETE}，
 * 这个页面立刻变成「任意表删除入口 + SQL 注入面」。
 *
 * <p>四重校验（文档 §5.6 加固方案 ①②③ 的落地）：
 * <ol>
 *   <li><b>白名单</b>：表名必须命中 {@code app.data-maintenance.cleanup.allowed-tables}（服务端配置，
 *       前端不可影响）。白名单默认只放行纯追加的日志/审计表。</li>
 *   <li><b>标识符正则</b>：{@code ^[a-z_][a-z0-9_]{0,62}$} —— 直接排除引号、分号、空格、注释符、
 *       大写（避免 PostgreSQL 的未加引号标识符折叠成小写后语义漂移）。</li>
 *   <li><b>存在性校验</b>：查 {@code information_schema} 确认表/列真实存在（防止白名单过期残留）。</li>
 *   <li><b>类型约束</b>：条件列必须是**时间类型**列（保留天数的语义只对时间列成立）。</li>
 * </ol>
 *
 * <p>通过四重校验后的标识符才允许拼接（拼之前再用双引号包裹）。**值**（保留天数）始终走
 * JDBC 参数绑定，绝不拼进 SQL。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupGuard {

    /** PostgreSQL 未加引号标识符的安全子集：小写字母/数字/下划线，且不以数字开头 */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-z_][a-z0-9_]{0,62}$");

    /** 允许作为清理条件的列类型（保留天数只对时间列有意义） */
    private static final Set<String> TEMPORAL_TYPES = Set.of(
            "timestamp without time zone",
            "timestamp with time zone",
            "date"
    );

    private final JdbcTemplate jdbcTemplate;
    private final DataMaintenanceProperties properties;

    /**
     * 校验目标表名
     *
     * @return 通过校验的表名（已小写、可直接安全拼接）
     * @throws IllegalArgumentException 任一校验不通过，消息可直接展示给用户
     */
    public String validateTable(String targetTable) {
        if (!StringUtils.hasText(targetTable)) {
            throw new IllegalArgumentException("规则未配置目标表，无法执行清理");
        }
        String table = targetTable.trim().toLowerCase(Locale.ROOT);
        if (!SAFE_IDENTIFIER.matcher(table).matches()) {
            throw new IllegalArgumentException("目标表名不合法（只允许小写字母/数字/下划线，且不以数字开头）: " + targetTable);
        }
        List<String> allowed = properties.getCleanup().getAllowedTables();
        boolean whitelisted = allowed != null && allowed.stream()
                .anyMatch(t -> t != null && t.trim().toLowerCase(Locale.ROOT).equals(table));
        if (!whitelisted) {
            throw new IllegalArgumentException("目标表「" + table + "」不在清理白名单内（app.data-maintenance.cleanup.allowed-tables）。"
                    + "为避免误删业务数据，清理动作只对服务端显式放行的表生效。");
        }
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                Integer.class, table);
        if (exists == null || exists == 0) {
            throw new IllegalArgumentException("目标表「" + table + "」在数据库中不存在");
        }
        return table;
    }

    /**
     * 校验条件列名
     *
     * @param table 已通过 {@link #validateTable} 的表名
     * @return 通过校验的列名
     * @throws IllegalArgumentException 任一校验不通过
     */
    public String validateConditionColumn(String table, String conditionColumn) {
        if (!StringUtils.hasText(conditionColumn)) {
            throw new IllegalArgumentException("规则未配置条件列，无法确定按哪一列判断保留期");
        }
        String column = conditionColumn.trim().toLowerCase(Locale.ROOT);
        if (!SAFE_IDENTIFIER.matcher(column).matches()) {
            throw new IllegalArgumentException("条件列名不合法（只允许小写字母/数字/下划线，且不以数字开头）: " + conditionColumn);
        }
        List<String> types = jdbcTemplate.queryForList(
                "SELECT data_type FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
                String.class, table, column);
        if (types.isEmpty()) {
            throw new IllegalArgumentException("条件列「" + column + "」在表「" + table + "」中不存在");
        }
        String dataType = types.get(0) == null ? "" : types.get(0).toLowerCase(Locale.ROOT);
        if (properties.getCleanup().isRequireTemporalColumn() && !TEMPORAL_TYPES.contains(dataType)) {
            throw new IllegalArgumentException("条件列「" + column + "」类型为 " + dataType
                    + "，不是时间类型列；按保留天数清理只对 timestamp/date 列成立");
        }
        return column;
    }

    /**
     * 校验保留天数
     *
     * @throws IllegalArgumentException 小于配置下限（retentionDays=0 等价于删空整表）
     */
    public int validateRetentionDays(Integer retentionDays) {
        if (retentionDays == null) {
            throw new IllegalArgumentException("规则未配置保留天数，无法执行清理");
        }
        int min = properties.getCleanup().getMinRetentionDays();
        if (retentionDays < min) {
            throw new IllegalArgumentException("保留天数 " + retentionDays + " 小于允许的最小值 " + min
                    + "（0 或负数会删除表内全部数据），已拒绝执行");
        }
        return retentionDays;
    }

    /** 该表是否为分区表（用于给出「大表应走分区删除」的官方建议回执） */
    public boolean isPartitioned(String table) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM pg_partitioned_table pt JOIN pg_class c ON c.oid = pt.partrelid WHERE c.relname = ?",
                    Integer.class, table);
            return count != null && count > 0;
        } catch (RuntimeException e) {
            log.warn("分区表探测失败（按未分区处理）: table={}, error={}", table, e.getMessage());
            return false;
        }
    }
}
