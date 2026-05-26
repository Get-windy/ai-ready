package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.service.impl.SysUserServiceImpl;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class SysUserServiceTest {

    @Mock
    private SysUserMapper userMapper;

    @InjectMocks
    private SysUserServiceImpl userService;

    private SysUser testUser;

    @BeforeEach
    void setUp() throws Exception {
        // 使用反射设置父类的 baseMapper 字段（遍历继承链查找）
        Field baseMapperField = null;
        Class<?> clazz = userService.getClass();
        while (clazz != null && baseMapperField == null) {
            try {
                baseMapperField = clazz.getDeclaredField("baseMapper");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (baseMapperField != null) {
            baseMapperField.setAccessible(true);
            baseMapperField.set(userService, userMapper);
        }
        
        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setTenantId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(BCrypt.hashpw("password123", BCrypt.gensalt()));
        testUser.setNickname("测试用户");
        testUser.setEmail("test@test.com");
        testUser.setPhone("13800138000");
        testUser.setStatus(0);
        testUser.setCreateTime(LocalDateTime.now());
        testUser.setUpdateTime(LocalDateTime.now());
    }

    @Test
    void testCreateUser_Success() {
        // given
        when(userMapper.selectByUsername(anyString(), anyLong())).thenReturn(null);
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);

        // when
        Long userId = userService.createUser(testUser);

        // then
        assertNotNull(userId);
        verify(userMapper).insert(any(SysUser.class));
    }

    @Test
    void testCreateUser_UsernameExists() {
        // given
        when(userMapper.selectByUsername(anyString(), anyLong())).thenReturn(testUser);

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(testUser);
        });
        assertEquals("用户名已存在", exception.getMessage());
    }

    @Test
    void testUpdateUser_Success() {
        // given
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // when
        userService.updateUser(testUser);

        // then
        verify(userMapper).updateById(any(SysUser.class));
    }

    @Test
    void testDeleteUser_Success() {
        // given
        when(userMapper.deleteById(anyLong())).thenReturn(1);

        // when
        userService.deleteUser(1L);

        // then
        verify(userMapper).deleteById(1L);
    }

    @Test
    void testBatchDeleteUsers_Success() {
        // given
        List<Long> userIds = Arrays.asList(1L, 2L, 3L);
        when(userMapper.deleteByIds(anyList())).thenReturn(3);

        // when
        userService.batchDeleteUsers(userIds);

        // then
        verify(userMapper).deleteByIds(userIds);
    }

    @Test
    void testResetPassword_Success() {
        // given
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // when
        userService.resetPassword(1L, "newPassword123");

        // then
        verify(userMapper).updateById(any(SysUser.class));
    }

    @Test
    void testChangePassword_Success() {
        // given
        when(userMapper.selectById(anyLong())).thenReturn(testUser);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // when
        userService.changePassword(1L, "password123", "newPassword123");

        // then
        verify(userMapper, times(1)).selectById(1L);
        verify(userMapper, times(1)).updateById(any(SysUser.class));
    }

    @Test
    void testChangePassword_WrongOldPassword() {
        // given
        when(userMapper.selectById(anyLong())).thenReturn(testUser);

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.changePassword(1L, "wrongPassword", "newPassword123");
        });
        assertEquals("原密码错误", exception.getMessage());
    }

    @Test
    void testPageUsers() {
        // given
        Page<SysUser> page = new Page<>(1, 10);
        when(userMapper.selectUserPage(any(Page.class), anyLong(), any(), any(), any()))
                .thenReturn(page);

        // when
        Page<SysUser> result = userService.pageUsers(page, 1L, "test", 0, null);

        // then
        assertNotNull(result);
        verify(userMapper).selectUserPage(page, 1L, "test", 0, null);
    }

    @Test
    void testGetUserDetail() {
        // given
        when(userMapper.selectById(anyLong())).thenReturn(testUser);

        // when
        SysUser result = userService.getUserDetail(1L);

        // then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testUpdateUserStatus() {
        // given
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // when
        userService.updateUserStatus(1L, 1);

        // then
        verify(userMapper).updateById(any(SysUser.class));
    }
}
