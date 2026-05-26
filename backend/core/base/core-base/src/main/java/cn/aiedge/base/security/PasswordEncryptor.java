package cn.aiedge.base.security;

import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 * 使用 BCrypt 算法进行密码加密和验证
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
public class PasswordEncryptor {

    /**
     * 加密密码
     * 
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 验证密码
     * 
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }

    /**
     * 判断密码是否需要重新加密
     * BCrypt 每次加密结果不同，这里简单判断格式
     * 
     * @param encodedPassword 加密后的密码
     * @return 是否需要重新加密
     */
    public boolean needsUpgrade(String encodedPassword) {
        // BCrypt 密码以 $2a$ 或 $2b$ 开头
        return encodedPassword == null || 
               (!encodedPassword.startsWith("$2a$") && !encodedPassword.startsWith("$2b$"));
    }
}