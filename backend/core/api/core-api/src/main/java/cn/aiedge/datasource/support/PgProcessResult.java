package cn.aiedge.datasource.support;

/**
 * PostgreSQL 客户端进程（pg_dump / pg_restore）的执行结果
 *
 * @param success    退出码为 0 且未超时/未抛异常
 * @param exitCode   进程退出码（超时或启动失败时为 -1）
 * @param output     合并后的 stdout+stderr（已按配置截断，用于失败原因的可见性）
 * @param durationMs 实际耗时毫秒
 * @param message    人类可读的结论/失败原因
 */
public record PgProcessResult(
        boolean success,
        int exitCode,
        String output,
        long durationMs,
        String message) {

    public static PgProcessResult fail(String message, long durationMs) {
        return new PgProcessResult(false, -1, "", durationMs, message);
    }
}
