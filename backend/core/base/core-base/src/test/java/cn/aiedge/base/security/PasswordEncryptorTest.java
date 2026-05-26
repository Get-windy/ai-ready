package cn.aiedge.base.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码加密器测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@DisplayName("密码加密器测试")
class PasswordEncryptorTest {

    private PasswordEncryptor passwordEncryptor;

    @BeforeEach
    void setUp() {
        passwordEncryptor = new PasswordEncryptor();
    }

    @Test
    @DisplayName("密码加密 - 成功")
    void testEncode() {
        String rawPassword = "admin123";
        String encodedPassword = passwordEncryptor.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$"));
    }

    @Test
    @DisplayName("密码验证 - 匹配")
    void testMatchesSuccess() {
        String rawPassword = "admin123";
        String encodedPassword = passwordEncryptor.encode(rawPassword);

        assertTrue(passwordEncryptor.matches(rawPassword, encodedPassword));
    }

    @Test
    @DisplayName("密码验证 - 不匹配")
    void testMatchesFailure() {
        String rawPassword = "admin123";
        String encodedPassword = passwordEncryptor.encode(rawPassword);

        assertFalse(passwordEncryptor.matches("wrongpassword", encodedPassword));
    }

    @Test
    @DisplayName("密码验证 - 空密码")
    void testMatchesNullPassword() {
        assertFalse(passwordEncryptor.matches(null, "$2a$10$xxxx"));
        assertFalse(passwordEncryptor.matches("password", null));
        assertFalse(passwordEncryptor.matches(null, null));
    }

    @Test
    @DisplayName("密码升级判断 - 需要升级")
    void testNeedsUpgradeTrue() {
        assertTrue(passwordEncryptor.needsUpgrade(null));
        assertTrue(passwordEncryptor.needsUpgrade("plainpassword"));
        assertTrue(passwordEncryptor.needsUpgrade("md5hash"));
    }

    @Test
    @DisplayName("密码升级判断 - 不需要升级")
    void testNeedsUpgradeFalse() {
        String encodedPassword = passwordEncryptor.encode("password");
        assertFalse(passwordEncryptor.needsUpgrade(encodedPassword));
    }

    @Test
    @DisplayName("每次加密结果不同")
    void testDifferentHashEachTime() {
        String rawPassword = "admin123";
        String encoded1 = passwordEncryptor.encode(rawPassword);
        String encoded2 = passwordEncryptor.encode(rawPassword);

        // BCrypt 每次加密结果不同（因为 salt 不同）
        assertNotEquals(encoded1, encoded2);

        // 但两个都能验证原始密码
        assertTrue(passwordEncryptor.matches(rawPassword, encoded1));
        assertTrue(passwordEncryptor.matches(rawPassword, encoded2));
    }
}