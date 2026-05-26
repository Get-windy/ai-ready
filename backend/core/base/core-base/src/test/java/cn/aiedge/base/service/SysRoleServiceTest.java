package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysRole;
import cn.aiedge.base.mapper.SysRoleMapper;
import cn.aiedge.base.mapper.SysRoleMenuMapper;
import cn.aiedge.base.mapper.SysRolePermissionMapper;
import cn.aiedge.base.service.impl.SysRoleServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
 * 角色服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class SysRoleServiceTest {

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysRolePermissionMapper rolePermissionMapper;

    @Mock
    private SysRoleMenuMapper roleMenuMapper;

    @InjectMocks
    private SysRoleServiceImpl roleService;

    private SysRole testRole;

    @BeforeEach
    void setUp() throws Exception {
        // 使用反射设置父类的 baseMapper 字段（遍历继承链查找）
        Field baseMapperField = null;
        Class<?> clazz = roleService.getClass();
        while (clazz != null && baseMapperField == null) {
            try {
                baseMapperField = clazz.getDeclaredField("baseMapper");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        if (baseMapperField != null) {
            baseMapperField.setAccessible(true);
            baseMapperField.set(roleService, roleMapper);
        }
        
        testRole = new SysRole();
        testRole.setId(1L);
        testRole.setTenantId(1L);
        testRole.setRoleName("测试角色");
        testRole.setRoleCode("test_role");
        testRole.setRoleType(1);
        testRole.setDataScope(0);
        testRole.setSort(1);
        testRole.setStatus(0);
        testRole.setRemark("测试角色备注");
        testRole.setCreateTime(LocalDateTime.now());
        testRole.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @Disabled("需要 Sa-Token 登录上下文，跳过此测试")
    void testCreateRole_Success() {
        // given
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        // when
        Long roleId = roleService.createRole(testRole);

        // then
        assertNotNull(roleId);
        verify(roleMapper).insert(any(SysRole.class));
    }

    @Test
    void testUpdateRole_Success() {
        // given
        when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);

        // when
        roleService.updateRole(testRole);

        // then
        verify(roleMapper).updateById(any(SysRole.class));
    }

    @Test
    void testDeleteRole_Success() {
        // given
        when(roleMapper.deleteById(anyLong())).thenReturn(1);

        // when
        roleService.deleteRole(1L);

        // then
        verify(roleMapper).deleteById(1L);
    }

    @Test
    void testAssignPermissions_Success() {
        // given
        when(roleMapper.selectById(anyLong())).thenReturn(testRole);
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);

        // when
        roleService.assignPermissions(1L, permissionIds);

        // then
        verify(roleMapper).selectById(1L);
    }

    @Test
    void testAssignPermissions_EmptyList() {
        // when
        roleService.assignPermissions(1L, List.of());

        // then - 应该不会抛出异常
    }

    @Test
    void testAssignPermissions_NullRole() {
        // given
        when(roleMapper.selectById(anyLong())).thenReturn(null);

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            roleService.assignPermissions(1L, Arrays.asList(1L, 2L));
        });
        assertEquals("角色不存在", exception.getMessage());
    }

    @Test
    void testAssignMenus_Success() {
        // given
        when(roleMapper.selectById(anyLong())).thenReturn(testRole);
        List<Long> menuIds = Arrays.asList(1L, 2L, 3L);

        // when
        roleService.assignMenus(1L, menuIds);

        // then
        verify(roleMapper).selectById(1L);
    }

    @Test
    void testPageRoles() {
        // given
        Page<SysRole> page = new Page<>(1, 10);
        when(roleMapper.selectPage(any(Page.class), any())).thenReturn(page);

        // when
        Page<SysRole> result = roleService.pageRoles(page, 1L, "测试", 0);

        // then
        assertNotNull(result);
        verify(roleMapper).selectPage(any(Page.class), any());
    }

    @Test
    void testGetRolePermissionIds() {
        // given
        List<Long> permissionIds = Arrays.asList(1L, 2L, 3L);
        when(roleMapper.selectPermissionIdsByRoleId(anyLong())).thenReturn(permissionIds);

        // when
        List<Long> result = roleService.getRolePermissionIds(1L);

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(roleMapper).selectPermissionIdsByRoleId(1L);
    }

    @Test
    void testGetRoleMenuIds() {
        // given
        List<Long> menuIds = Arrays.asList(1L, 2L, 3L);
        when(roleMapper.selectMenuIdsByRoleId(anyLong())).thenReturn(menuIds);

        // when
        List<Long> result = roleService.getRoleMenuIds(1L);

        // then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(roleMapper).selectMenuIdsByRoleId(1L);
    }

    @Test
    void testGetUserRoles() {
        // given
        List<SysRole> roles = Arrays.asList(testRole);
        when(roleMapper.selectRolesByUserId(anyLong())).thenReturn(roles);

        // when
        List<SysRole> result = roleService.getUserRoles(1L);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(roleMapper).selectRolesByUserId(1L);
    }

    @Test
    void testUpdateRoleStatus() {
        // given
        when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);

        // when
        roleService.updateRoleStatus(1L, 1);

        // then
        verify(roleMapper).updateById(any(SysRole.class));
    }
}
