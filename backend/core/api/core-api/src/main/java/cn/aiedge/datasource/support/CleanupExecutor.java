package cn.aiedge.datasource.support;

import cn.aiedge.datasource.config.DataMaintenanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 清理执行器：按保留天数删除目标表历史数据
 *
 * <p>⚠️ 取舍说明（文档 §8.5 已登记「明确不借鉴把 DELETE 当大数据量清理主要手段」）：
 * PostgreSQL 官方对大表的推荐做法是 <b>分区表走 {@code DETACH PARTITION} / {@code DROP PARTITION}</b>
 * —— 直接摘掉/丢弃分区是元数据操作，几乎瞬时完成，也不会因删除大量行而膨胀表文件与 WAL
 * （删除的每一行都要写 WAL 记录，且空间不会立即归还，需要 autovacuum 回收）。
 *
 * <p>本系统的现状（2026-09-19 psql 实测）：{@code pg_partitioned_table} 计数为 <b>0</b> ——
 * 目标库**没有任何分区表**，因此 {@code DETACH/DROP PARTITION} 路径当前无对象可用；
 * 且在非分区表上模拟分区删除只能靠 {@code TRUNCATE}/{@code DROP}，那是整表级破坏，
 * 与「按保留期滚动删除」语义不符。
 *
 * <p>所以首版采取：<b>分批 {@code DELETE}</b>（每批 {@link DataMaintenanceProperties.Cleanup#getBatchSize()} 行）
 * + <b>单次上限</b>（{@link DataMaintenanceProperties.Cleanup#getMaxRowsPerRun()} 行，达上限即停并回报截断）。
 * 分批而不是一条大 DELETE 的理由：单条大 DELETE 会长时间持锁并生成一个巨型事务，
 * 分批把锁持有时间与事务体积压到可控范围。
 *
 * <p>二期（未闭环，已在文档 §9.3 登记）：把日志/审计表按月份分区改造，
 * 届时本执行器改为优先走 {@code DETACH PARTITION}，{@code DELETE} 仅作兜底。
 * {@link #isPartitioned} 的探测结果会随每次执行回执，便于判断何时可以切换。
 *
 * <p>SQL 构造安全：表名/列名必须**先**通过 {@link CleanupGuard} 的四重校验，
 * 拼进 SQL 时再用双引号包裹；保留天数始终走 JDBC 参数绑定（{@code make_interval(days => ?)}），
 * 批大小是服务端配置（非用户输入），经下界校验后内联。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanupExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final DataMaintenanceProperties properties;
    private final CleanupGuard cleanupGuard;

    /**
     * 预统计：满足删除条件的行数（干跑口径，不删任何数据）
     *
     * <p>把「会删多少」先算出来，既用于页面的二次确认，也用于执行后比对。
     */
    public long countMatched(String table, String column, int retentionDays) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + quote(table) + " WHERE " + quote(column) + " IS NOT NULL"
                        + " AND " + quote(column) + " < now() - make_interval(days => ?)",
                Long.class, retentionDays);
        return count == null ? 0L : count;
    }

    /**
     * 执行分批删除
     *
     * @param table         已通过 {@link CleanupGuard#validateTable} 的表名
     * @param column        已通过 {@link CleanupGuard#validateConditionColumn} 的列名
     * @param retentionDays 已通过 {@link CleanupGuard#validateRetentionDays} 的保留天数
     */
    public CleanupOutcome execute(String table, String column, int retentionDays) {
        long start = System.currentTimeMillis();
        int batchSize = Math.max(1, properties.getCleanup().getBatchSize());
        long maxRows = Math.max(1L, properties.getCleanup().getMaxRowsPerRun());
        boolean partitioned = cleanupGuard.isPartitioned(table);

        long planned = countMatched(table, column, retentionDays);
        long deleted = 0L;
        boolean truncated = false;

        while (deleted < maxRows) {
            int batch = (int) Math.min(batchSize, maxRows - deleted);
            // ctid 批删：目标表可能没有单列主键（日志表常见），按 ctid 定位是最通用的做法。
            // 仍用子查询 LIMIT 而不是一条大 DELETE —— 见类注释的分批理由。
            String sql = "DELETE FROM " + quote(table)
                    + " WHERE ctid IN (SELECT ctid FROM " + quote(table)
                    + " WHERE " + quote(column) + " IS NOT NULL"
                    + " AND " + quote(column) + " < now() - make_interval(days => ?)"
                    + " LIMIT " + batch + ")";
            int rows = jdbcTemplate.update(sql, retentionDays);
            deleted += rows;
            if (rows < batch) {
                // 本批没删满 → 条件下已无更多行
                break;
            }
            if (deleted >= maxRows) {
                truncated = countMatched(table, column, retentionDays) > 0;
                break;
            }
        }

        long remaining = countMatched(table, column, retentionDays);
        long duration = System.currentTimeMillis() - start;

        StringBuilder msg = new StringBuilder();
        msg.append("表 ").append(table)
                .append("，条件列 ").append(column)
                .append("，保留 ").append(retentionDays).append(" 天；")
                .append("预统计 ").append(planned).append(" 行，实删 ").append(deleted).append(" 行，")
                .append("剩余 ").append(remaining).append(" 行，耗时 ").append(duration).append("ms");
        if (truncated) {
            msg.append("；⚠️ 已达单次上限 ").append(maxRows).append(" 行，本批剩余数据未删除（可再次执行继续）");
        }
        if (partitioned) {
            msg.append("；ℹ️ 该表为分区表，PostgreSQL 官方推荐用 DETACH/DROP PARTITION 代替批量 DELETE");
        }
        if (planned != deleted && !truncated) {
            msg.append("；ℹ️ 预统计与实际删除行数不一致（差值 ").append(planned - deleted)
                    .append("），可能存在并发写入或统计期间的数据变化");
        }

        log.warn("清理执行完成: {}", msg);
        return new CleanupOutcome(table, column, retentionDays, planned, deleted, remaining,
                truncated, partitioned, duration, msg.toString());
    }

    /**
     * 包装标识符：必须已通过 {@link CleanupGuard} 的正则 + 白名单 + 存在性校验；
     * 这里再加双引号是「纵深防御」—— 即使前置换掉，引号也能阻断注入串
     */
    private String quote(String identifier) {
        return "\"" + identifier + "\"";
    }
}
