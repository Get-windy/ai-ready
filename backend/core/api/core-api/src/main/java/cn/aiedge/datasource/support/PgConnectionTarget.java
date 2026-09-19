package cn.aiedge.datasource.support;

/**
 * pg_dump / pg_restore 的目标连接信息
 *
 * <p>⚠️ {@link #password} 是敏感值，**仅**通过 {@code PGPASSWORD} 环境变量传给子进程，
 * 绝不拼进命令行参数（命令行在多数系统上对同机其它用户可见，且会被写进进程列表与审计日志）。
 *
 * <p>{@link #description} 用于回显「本次到底备份了哪个库」，让页面不必猜测 —— 这是
 * 「不谎报」的一部分：用户看到的目标必须与实际执行的目标一致。
 *
 * @param host        主机
 * @param port        端口
 * @param database    数据库名
 * @param username    用户名
 * @param password    口令（可为空串，交由 pg 客户端自身的 .pgpass / trust 认证处理）
 * @param origin      连接信息来源，取值 {@code sys_data_source} 或 {@code spring.datasource}
 * @param description 人类可读目标描述，如 {@code localhost:5432/devdb (来自 spring.datasource)}
 */
public record PgConnectionTarget(
        String host,
        int port,
        String database,
        String username,
        String password,
        String origin,
        String description) {

    /** 供日志/页面回显，永不包含口令 */
    public String safeDescription() {
        return description;
    }
}
