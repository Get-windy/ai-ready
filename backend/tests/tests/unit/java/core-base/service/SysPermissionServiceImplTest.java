package cn.aiedge.base.service.impl;

import cn.aiedge.base.entity.SysPermission;
import cn.aiedge.base.mapper.SysPermissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 权限服务实现类单元测试
 * 
 * @author AI-Ready QA Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysPermissionServiceImpl - 权限服务单元测试")
class SysPermissionServiceImplTest {

    @Mock
    private SysPermissionMapper permissionMapper;

    @InjectMocks
    private SysPermissionServiceImpl permissionService;

    private SysPermission testPermission;
    private final Long tenantId = 1L;

    @BeforeEach
    void setUp() {
        testPermission = new SysPermission();
        testPermission.setId(1L);
        testPermission.setPermissionCode("user:create");
        testPermission.setPermissionName("创建用户");
        testPermission.setPermissionType(1);
        testPermission.setParentId(0L);
        testPermission.setTenantId(tenantId);
        testPermission.setStatus(0);
        testPermission.setSort(1);
    }

    // ==================== 创建权限测试 ====================

    @Test
    @DisplayName("创建权限 - 成功场景")
    void testCreatePermission_Success() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.insert(any(SysPermission.class))).thenReturn(1);

        // When
        Long permissionId = permissionService.createPermission(testPermission);

        // Then
        assertNotNull(permissionId);
        assertEquals(1L, permissionId);
        verify(permissionMapper).insert(any(SysPermission.class));
    }

    @Test
    @DisplayName("创建权限 - 权限编码已存在")
    void testCreatePermission_CodeExists() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.createPermission(testPermission);
        });
        assertEquals("权限编码已存在", exception.getMessage());
    }

    @Test
    @DisplayName("创建权限 - 自动设置默认值")
    void testCreatePermission_DefaultValues() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.insert(any(SysPermission.class))).thenReturn(1);

        // When
        permissionService.createPermission(testPermission);

        // Then
        assertEquals(0, testPermission.getStatus());
        assertNotNull(testPermission.getCreateTime());
        assertNotNull(testPermission.getUpdateTime());
    }

    // ==================== 更新权限测试 ====================

    @Test
    @DisplayName("更新权限 - 成功场景")
    void testUpdatePermission_Success() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.updateById(any(SysPermission.class))).thenReturn(1);

        // When
        assertDoesNotThrow(() -> permissionService.updatePermission(testPermission));

        // Then
        verify(permissionMapper).updateById(any(SysPermission.class));
    }

    @Test
    @DisplayName("更新权限 - 编码重复（排除自身）")
    void testUpdatePermission_CodeExistsExcludeSelf() {
        // Given
        testPermission.setId(2L);
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.updatePermission(testPermission);
        });
        assertEquals("权限编码已存在", exception.getMessage());
    }

    @Test
    @DisplayName("更新权限 - 自动更新时间")
    void testUpdatePermission_UpdateTime() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.updateById(any(SysPermission.class))).thenReturn(1);

        // When
        permissionService.updatePermission(testPermission);

        // Then
        assertNotNull(testPermission.getUpdateTime());
    }

    // ==================== 删除权限测试 ====================

    @Test
    @DisplayName("删除权限 - 成功场景")
    void testDeletePermission_Success() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.deleteById(1L)).thenReturn(1);

        // When
        assertDoesNotThrow(() -> permissionService.deletePermission(1L));

        // Then
        verify(permissionMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除权限 - 存在子权限")
    void testDeletePermission_HasChildren() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.deletePermission(1L);
        });
        assertEquals("存在子权限，无法删除", exception.getMessage());
    }

    // ==================== 批量删除权限测试 ====================

    @Test
    @DisplayName("批量删除权限 - 成功场景")
    void testBatchDeletePermissions_Success() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.deleteBatchIds(ids)).thenReturn(3);

        // When
        assertDoesNotThrow(() -> permissionService.batchDeletePermissions(ids));

        // Then
        verify(permissionMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除权限 - 部分有子权限")
    void testBatchDeletePermissions_SomeHasChildren() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L);
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            permissionService.batchDeletePermissions(ids);
        });
        assertEquals("部分权限存在子权限，无法删除", exception.getMessage());
    }

    // ==================== 分页查询权限测试 ====================

    @Test
    @DisplayName("分页查询权限 - 全部条件")
    void testPagePermissions_AllConditions() {
        // Given
        Page<SysPermission> page = new Page<>(1, 10);
        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        Page<SysPermission> result = permissionService.pagePermissions(
            page, tenantId, "用户", 1, 0);

        // Then
        assertNotNull(result);
        verify(permissionMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分页查询权限 - 仅租户ID")
    void testPagePermissions_OnlyTenantId() {
        // Given
        Page<SysPermission> page = new Page<>(1, 10);
        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        // When
        Page<SysPermission> result = permissionService.pagePermissions(
            page, tenantId, null, null, null);

        // Then
        assertNotNull(result);
    }



    // ==================== 获取权限详情测试 ====================

    @Test
    @DisplayName("获取权限详情 - 成功场景")
    void testGetPermissionDetail_Success() {
        // Given
        when(permissionMapper.selectById(1L)).thenReturn(testPermission);

        // When
        SysPermission result = permissionService.getById(1L);

        // Then
        assertNotNull(result);
        assertEquals("user:create", result.getPermissionCode());
        verify(permissionMapper).selectById(1L);
    }

    @Test
    @DisplayName("获取权限详情 - 权限不存在")
    void testGetPermissionDetail_NotFound() {
        // Given
        when(permissionMapper.selectById(999L)).thenReturn(null);

        // When
        SysPermission result = permissionService.getById(999L);

        // Then
        assertNull(result);
    }

    // ==================== 检查权限编码是否存在测试 ====================

    @Test
    @DisplayName("检查权限编码 - 编码存在")
    void testCheckPermissionCodeExists_Exists() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        // When
        boolean exists = permissionService.checkPermissionCodeExists("user:create", tenantId, null);

        // Then
        assertTrue(exists);
    }

    @Test
    @DisplayName("检查权限编码 - 编码不存在")
    void testCheckPermissionCodeExists_NotExists() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean exists = permissionService.checkPermissionCodeExists("user:delete", tenantId, null);

        // Then
        assertFalse(exists);
    }

    @Test
    @DisplayName("检查权限编码 - 排除自身")
    void testCheckPermissionCodeExists_ExcludeSelf() {
        // Given
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // When
        boolean exists = permissionService.checkPermissionCodeExists("user:create", tenantId, 1L);

        // Then
        assertFalse(exists);
    }

}
