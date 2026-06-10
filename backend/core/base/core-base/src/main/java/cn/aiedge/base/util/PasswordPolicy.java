package cn.aiedge.base.util;

import lombok.extern.slf4j.Slf4j;

/**
 * 密码策略工具类
 * <p>
 * 定义密码复杂度规则：
 * <ul>
 *   <li>长度 8-64 字符</li>
 *   <li>至少包含大写字母、小写字母、数字、特殊字符中的 3 种</li>
 *   <li>不能包含空白字符</li>
 * </ul>
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class PasswordPolicy {

    private PasswordPolicy() {}

    /** 最小长度 */
    public static final int MIN_LENGTH = 8;
    /** 最大长度 */
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

    /**
     * 验证密码是否符合策略
     *
     * @param password 明文密码
     * @return 如果密码合规返回 null，否则返回错误描述
     */
    public static String validate(String password) {
        if (password == null || password.isEmpty()) {
            return "密码不能为空";
        }

        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return "密码长度必须在 " + MIN_LENGTH + "-" + MAX_LENGTH + " 个字符之间";
        }

        if (password.matches(WHITESPACE)) {
            return "密码不能包含空白字符";
        }

        // 统计包含的字符类别数
        int categoryCount = 0;
        if (password.matches(UPPER)) categoryCount++;
        if (password.matches(LOWER)) categoryCount++;
        if (password.matches(DIGIT)) categoryCount++;
        if (password.matches(SPECIAL)) categoryCount++;

        if (categoryCount < 3) {
            return "密码必须至少包含大写字母、小写字母、数字、特殊字符中的 3 种";
        }

        return null; // 合规
    }

    /**
     * 生成密码强度描述
     */
    public static String getStrengthDescription() {
        return "密码长度 " + MIN_LENGTH + "-" + MAX_LENGTH + " 位，"
                + "必须包含大写字母、小写字母、数字、特殊字符中的至少 3 种";
    }
}
