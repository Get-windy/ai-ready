package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysUser;
import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务单元测试
 * 由于 MyBatis-Plus ServiceImpl 的 baseMapper 是 protected，
 * 这里只测试基本的实体操作和密码加密逻辑
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@DisplayName("用户服务测试")
class SysUserServiceTest {

    private SysUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setTenantId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(BCrypt.hashpw("password123", BCrypt.gensalt()));
        testUser.setNickname("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setPhone("13800138000");
        testUser.setStatus(0);
    }

    @Test
    @DisplayName("用户实体 - 属性设置")
    void testUserEntityProperties() {
        assertNotNull(testUser.getId());
        assertEquals(1L, testUser.getId());
        assertEquals("testuser", testUser.getUsername());
        assertEquals("测试用户", testUser.getNickname());
        assertEquals("test@example.com", testUser.getEmail());
        assertEquals(0, testUser.getStatus());
    }

    @Test
    @DisplayName("密码加密 - BCrypt验证")
    void testPasswordEncryption() {
        String rawPassword = "password123";
        String hashedPassword = testUser.getPassword();

        assertTrue(BCrypt.checkpw(rawPassword, hashedPassword));
        assertFalse(BCrypt.checkpw("wrongpassword", hashedPassword));
    }

    @Test
    @DisplayName("用户链式设置")
    void testUserChainSetter() {
        SysUser user = new SysUser()
                .setId(2L)
                .setUsername("newuser")
                .setNickname("新用户")
                .setEmail("new@example.com");

        assertEquals(2L, user.getId());
        assertEquals("newuser", user.getUsername());
        assertEquals("新用户", user.getNickname());
    }

    @Test
    @DisplayName("密码生成 - 随机salt")
    void testPasswordGeneration() {
        String password1 = BCrypt.hashpw("password", BCrypt.gensalt());
        String password2 = BCrypt.hashpw("password", BCrypt.gensalt());

        // 相同密码不同salt应该产生不同hash
        assertNotEquals(password1, password2);

        // 但都能验证原始密码
        assertTrue(BCrypt.checkpw("password", password1));
        assertTrue(BCrypt.checkpw("password", password2));
    }
}