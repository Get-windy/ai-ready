package cn.aiedge.base.util;

import cn.aiedge.base.spi.PlatformSecuritySettingsProvider;
import cn.aiedge.base.spi.PlatformSecuritySettingsProvider.PlatformSecuritySettings;
import lombok.extern.slf4j.Slf4j;

/**
 * 密码策略工具类。
 *
 * <p><b>规则来源（2026-09-19 接线）</b>，按优先级：
 * <ol>
 *   <li><b>平台级数据库策略</b> {@code sys_security_policy}（「系统 → 平台设置 → 安全策略」菜单 62505 维护）
 *       —— 经 {@link PlatformSecuritySettingsProvider} 读取。提供最小长度与四类字符的**开关**；</li>
 *   <li><b>内置默认值</b>（改造前的行为）—— 长度 8-64，且四类字符中**至少满足 3 类**。
 *       未配置策略、策略未启用、或读取失败时回退到这里，保证不会"因为读不到配置就把系统锁死"。</li>
 * </ol>
 *
 * <p><b>为什么用静态持有 provider</b>：{@code validate} 有 3 处静态调用点
 * （{@code SysUserServiceImpl} 两处、{@code TenantRegistrationService} 一处），
 * 改成实例方法要动全部调用点与它们的构造。这里改为由 {@link SecurityPolicyBridge}
 * 在启动时把 provider 注入静态字段，调用点保持原样。
 *
 * <p><b>本类不做的两件事（避免给出"配了但没用"的假象）</b>：
 * <ul>
 *   <li>不实现 {@code passwordExpireDays} —— 它由 {@code SysUserServiceImpl} 的
 *       有效期判定消费，不在本类；</li>
 *   <li>不实现 {@code lockThreshold}/{@code lockDuration}/{@code sessionTimeout}/{@code ipWhitelist}
 *       / {@code singleDevice} —— 这些字段目前**仍无消费方**，见开发文档「剩余缺口」。</li>
 * </ul>
 */
@Slf4j
public class PasswordPolicy {

    private PasswordPolicy() {}

    /** 内置默认：最小长度 */
    public static final int MIN_LENGTH = 8;
    /** 内置默认：最大长度 */
    public static final int MAX_LENGTH = 64;

    /** 大写字母 */
    private static final String UPPER = ".*[A-Z].*";
    /** 小写字母 */
    private static final String LOWER = ".*[a-z].*";
    /** 数字 */
    private static final String DIGIT = ".*\\d.*";
    /** 特殊字符 */
    private static final String SPECIAL = ".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~].*";
    /** 空白字符 */
    private static final String WHITESPACE = ".*\\s.*";

    /** 内置默认：四类字符中至少满足的类别数 */
    private static final int DEFAULT_REQUIRED_CATEGORIES = 3;

    /**
     * 平台策略读取口。
     * <p>由 {@link SecurityPolicyBridge} 在启动时注入；未注入（如单元测试、裁剪部署）时为 null，
     * 此时按内置默认规则校验。
     */
    static volatile PlatformSecuritySettingsProvider provider;

    /**
     * 由 {@code cn.aiedge.base.config.SecurityPolicyBridge} 在启动时调用。
     *
     * <p>设为 public 是因为调用方在**另一个包**（config）里 —— 本类的定位是"被系统各处静态调用"的工具类，
     * 这个注入点是它与 Spring 容器唯一的接缝，不该为了可见性另开一个包。
     * 除桥接组件外**不要**在别处调用。
     *
     * @param p 平台策略读取口；传 {@code null} 表示回退内置默认规则
     */
    public static void setProvider(PlatformSecuritySettingsProvider p) {
        provider = p;
    }

    /**
     * 验证密码是否符合策略。
     *
     * @param password 明文密码
     * @return 如果密码合规返回 null，否则返回错误描述
     */
    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }

        PlatformSecuritySettings policy = currentPolicy();
        int minLength = MIN_LENGTH;
        if (policy != null && policy.passwordMinLength() != null && policy.passwordMinLength() > 0) {
            minLength = policy.passwordMinLength();
        }

        if (password.length() < minLength || password.length() > MAX_LENGTH) {
            return "密码长度必须在 " + minLength + "-" + MAX_LENGTH + " 个字符之间";
        }

        if (password.matches(WHITESPACE)) {
            return "密码不能包含空白字符";
        }

        return checkCategories(password, policy);
    }

    /**
     * 字符类别校验。
     *
     * <p>两套语义，取决于是否有平台策略：
     * <ul>
     *   <li><b>有策略</b> —— 按**逐项开关**判定：勾了哪类就必须含哪类。此时不再套用
     *       "至少 3 类"的默认规则（否则用户把四个开关全关掉也过不了，与页面语义矛盾）。</li>
     *   <li><b>无策略</b> —— 沿用内置默认：四类中至少 3 类。</li>
     * </ul>
     */
    private static String checkCategories(String password, PlatformSecuritySettings policy) {
        if (policy == null) {
            int categoryCount = 0;
            if (password.matches(UPPER)) categoryCount++;
            if (password.matches(LOWER)) categoryCount++;
            if (password.matches(DIGIT)) categoryCount++;
            if (password.matches(SPECIAL)) categoryCount++;

            if (categoryCount < DEFAULT_REQUIRED_CATEGORIES) {
                return "密码必须至少包含大写字母、小写字母、数字、特殊字符中的 3 种";
            }
            return null;
        }

        StringBuilder missing = new StringBuilder();
        if (policy.requireUpper() && !password.matches(UPPER)) missing.append("大写字母、");
        if (policy.requireLower() && !password.matches(LOWER)) missing.append("小写字母、");
        if (policy.requireDigit() && !password.matches(DIGIT)) missing.append("数字、");
        if (policy.requireSpecial() && !password.matches(SPECIAL)) missing.append("特殊字符、");

        if (missing.length() > 0) {
            missing.setLength(missing.length() - 1); // 去掉末尾顿号
            return "密码必须包含：" + missing;
        }
        return null;
    }

    /**
     * 生成密码强度描述（用于错误提示，须与 {@link #validate} 的实际规则一致）。
     */
    public static String getStrengthDescription() {
        PlatformSecuritySettings policy = currentPolicy();
        if (policy == null) {
            return "密码长度 " + MIN_LENGTH + "-" + MAX_LENGTH + " 位，"
                    + "必须包含大写字母、小写字母、数字、特殊字符中的至少 3 种";
        }

        int minLength = (policy.passwordMinLength() != null && policy.passwordMinLength() > 0)
                ? policy.passwordMinLength() : MIN_LENGTH;
        StringBuilder required = new StringBuilder();
        if (policy.requireUpper()) required.append("大写字母、");
        if (policy.requireLower()) required.append("小写字母、");
        if (policy.requireDigit()) required.append("数字、");
        if (policy.requireSpecial()) required.append("特殊字符、");

        if (required.length() == 0) {
            return "密码长度 " + minLength + "-" + MAX_LENGTH + " 位，无字符类别要求";
        }
        required.setLength(required.length() - 1);
        return "密码长度 " + minLength + "-" + MAX_LENGTH + " 位，必须包含：" + required;
    }

    /**
     * 取当前生效的平台策略。
     *
     * <p>每次都实时读（由实现方决定是否缓存）—— 这样"在页面上改了策略"**无需重启即生效**。
     * 任何异常都吞掉并返回 null 走内置默认：**密码策略读不到时不能把所有人都挡在门外**。
     */
    private static PlatformSecuritySettings currentPolicy() {
        PlatformSecuritySettingsProvider p = provider;
        if (p == null) {
            return null;
        }
        try {
            return p.currentSecuritySettings();
        } catch (Exception e) {
            log.warn("读取平台安全策略失败，按内置默认密码规则校验", e);
            return null;
        }
    }
}
