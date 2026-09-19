package cn.aiedge.base.spi;

/**
 * 平台级邮件配置的读取接口（SPI）。
 *
 * <p><b>为什么要有这个接口</b>：邮件配置存在 `sys_mail_config` 表里，由
 * 「系统 → 平台设置 → 邮件配置」（菜单 62502）维护；而真正发信的是
 * `core-notification` 模块的 {@code EmailChannel}。
 * 依赖方向是 {@code core-api → core-notification → core-base}，
 * `core-notification` **不能**反向依赖 `core-api`，所以把读取口放在中间的 `core-base`，
 * 由 `core-api` 提供实现（Spring 自动注入）。
 *
 * <p><b>改造前的问题</b>：{@code EmailChannel} 只读 `spring.mail.*`（application yml），
 * 而本仓库的 yml 里根本没有 `spring.mail.*` → {@code JavaMailSender} 不会被创建 →
 * `EmailChannel` 因 {@code @ConditionalOnBean} 从不注册 → **邮件通知整体不可用**，
 * 且页面上保存的配置毫无作用。接入本接口后，页面保存的配置成为**第一优先**数据源。
 *
 * <p>实现方约定：**不得抛异常**。任何异常都要内部消化并返回 {@code null}，
 * 否则会把「读配置失败」放大成「发通知失败」。
 */
public interface PlatformMailSettingsProvider {

    /**
     * 取当前生效的邮件配置。
     *
     * @return 配置；未配置或读取失败时返回 {@code null}（调用方应回退到 yml / 判定不可用）
     */
    PlatformMailSettings currentMailSettings();

    /**
     * 平台级邮件配置快照。
     *
     * @param enabled    是否启用（未启用的配置一律视为不可用，避免误发信）
     * @param host       SMTP 主机
     * @param port       SMTP 端口
     * @param username   认证用户名
     * @param password   认证密码
     * @param fromAddress 发件人地址（为空时回退到 username）
     * @param encryption 加密方式：{@code SSL} / {@code TLS} / 其它（按明文处理）
     */
    record PlatformMailSettings(
            boolean enabled,
            String host,
            Integer port,
            String username,
            String password,
            String fromAddress,
            String encryption
    ) {
        /** 是否具备发信的最低条件 */
        public boolean usable() {
            return enabled && host != null && !host.isBlank();
        }
    }
}
