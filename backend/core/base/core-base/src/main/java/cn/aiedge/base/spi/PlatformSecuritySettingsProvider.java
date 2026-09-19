package cn.aiedge.base.spi;

/**
 * 平台级安全策略的读取接口（SPI）。
 *
 * <p>策略存在 `sys_security_policy` 表，由「系统 → 平台设置 → 安全策略」（菜单 62505）维护。
 * 消费方在 `core-base`（{@link cn.aiedge.base.util.PasswordPolicy}、
 * {@code SysUserServiceImpl} 的密码有效期判定），
 * 为了让 `core-api` 提供的实现能被它们读到，接口放在中间的 `core-base`。
 *
 * <p><b>改造前的问题</b>：本表全仓**无任何读取方** —— 页面上保存的策略
 * （密码复杂度、有效期、登录锁定、会话超时…）**不影响任何运行时行为**，
 * 而 `PasswordPolicy` 用的是**硬编码常量**（长度 8-64、四类字符至少 3 种）。
 *
 * <p><b>真实来源与兜底</b>：本接口返回的策略**优先于**代码内置默认值；
 * 返回 {@code null} 或 {@code enabled=false} 时，消费方一律回退到**改造前的内置规则**，
 * 保证「没配策略」与「配了不启用」都不会把系统锁死。
 *
 * <p>实现方约定：**不得抛异常**，失败一律返回 {@code null}。
 */
public interface PlatformSecuritySettingsProvider {

    /**
     * 取当前生效的安全策略。
     *
     * @return 策略；未配置、未启用或读取失败时返回 {@code null}
     */
    PlatformSecuritySettings currentSecuritySettings();

    /**
     * 平台级安全策略快照（只含**已接入消费方**的字段，避免给出"配了但不生效"的假象）。
     *
     * @param passwordMinLength   密码最小长度
     * @param requireUpper        必须含大写字母
     * @param requireLower        必须含小写字母
     * @param requireDigit        必须含数字
     * @param requireSpecial      必须含特殊字符
     * @param passwordExpireDays  密码有效期（天），{@code null} 或 {@code <=0} 表示不过期
     */
    record PlatformSecuritySettings(
            Integer passwordMinLength,
            boolean requireUpper,
            boolean requireLower,
            boolean requireDigit,
            boolean requireSpecial,
            Integer passwordExpireDays
    ) {
    }
}
