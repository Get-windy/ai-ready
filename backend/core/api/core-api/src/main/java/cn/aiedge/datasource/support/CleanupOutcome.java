package cn.aiedge.datasource.support;

/**
 * 一次清理执行的结果
 *
 * <p>刻意把「预统计行数」与「实际删除行数」分开记录：两者不一致说明执行期间有并发写入，
 * 是运维必须看到的信号；也是文档 §5.6 第 ⑧ 条「干跑/预演」能力的返回值形态。
 *
 * @param table          目标表（已通过白名单校验）
 * @param column         条件列（已通过时间类型校验）
 * @param retentionDays  保留天数
 * @param plannedRows    执行前预统计：满足「早于保留边界」的行数
 * @param deletedRows    实际删除行数
 * @param remainingRows  执行后复统计：仍满足条件的行数（>0 表示触及单次上限而截断）
 * @param truncated      是否因达到单次上限而截断
 * @param partitioned    目标表是否为分区表
 * @param durationMs     耗时
 * @param message        结论摘要（写入 last_run_result）
 */
public record CleanupOutcome(
        String table,
        String column,
        int retentionDays,
        long plannedRows,
        long deletedRows,
        long remainingRows,
        boolean truncated,
        boolean partitioned,
        long durationMs,
        String message) {

    /** 是否成功（含「达到上限而截断」的部分成功） */
    public boolean success() {
        return deletedRows >= 0;
    }
}
