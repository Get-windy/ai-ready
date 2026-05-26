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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SysPermissionServiceImpl - 批量操作和分页查询边界测试
 * 
 * @author AI-Ready QA Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SysPermissionServiceImpl - 边界测试")
class SysPermissionServiceImplBoundaryTest {

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

    // ==================== 批量删除边界测试 ====================

    @Test
    @DisplayName("批量删除权限 - 空列表")
    void testBatchDeletePermissions_EmptyList() {
        List<Long> emptyList = Collections.emptyList();
        
        // 空列表应该直接返回或抛出异常
        assertDoesNotThrow(() -> permissionService.batchDeletePermissions(emptyList));
    }

    @Test
    @DisplayName("批量删除权限 - 单条记录")
    void testBatchDeletePermissions_SingleItem() {
        List<Long> ids = Collections.singletonList(1L);
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.deleteBatchIds(ids)).thenReturn(1);

        assertDoesNotThrow(() -> permissionService.batchDeletePermissions(ids));
        verify(permissionMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除权限 - 大量记录")
    void testBatchDeletePermissions_LargeList() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 1000; i++) {
            ids.add(i);
        }
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.deleteBatchIds(ids)).thenReturn(1000);

        assertDoesNotThrow(() -> permissionService.batchDeletePermissions(ids));
        verify(permissionMapper).deleteBatchIds(ids);
    }

    @Test
    @DisplayName("批量删除权限 - 包含null值")
    void testBatchDeletePermissions_WithNull() {
        List<Long> ids = new ArrayList<>();
        ids.add(1L);
        ids.add(null);
        ids.add(2L);
        
        // 应该处理null值
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.deleteBatchIds(any())).thenReturn(2);

        assertDoesNotThrow(() -> permissionService.batchDeletePermissions(ids));
    }

    @Test
    @DisplayName("批量删除权限 - 重复ID")
    void testBatchDeletePermissions_DuplicateIds() {
        List<Long> ids = Arrays.asList(1L, 1L, 2L, 2L, 3L);
        
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.deleteBatchIds(any())).thenReturn(3);

        assertDoesNotThrow(() -> permissionService.batchDeletePermissions(ids));
    }

    @Test
    @DisplayName("批量删除权限 - 包含不存在的ID")
    void testBatchDeletePermissions_NonExistentIds() {
        List<Long> ids = Arrays.asList(9999L, 9998L, 9997L);
        
        when(permissionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(permissionMapper.deleteBatchIds(ids)).thenReturn(0);

        assertDoesNotThrow(() -> permissionService.batchDeletePermissions(ids));
    }

    // ==================== 分页查询边界测试 ====================

    @Test
    @DisplayName("分页查询 - 第0页(边界)")
    void testPagePermissions_PageZero() {
        Page<SysPermission> page = new Page<>(0, 10);
        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        Page<SysPermission> result = permissionService.pagePermissions(
            page, tenantId, null, null, null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("分页查询 - 第1页")
    void testPagePermissions_PageOne() {
        Page<SysPermission> page = new Page<>(1, 10);
        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        Page<SysPermission> result = permissionService.pagePermissions(
            page, tenantId, null, null, null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("分页查询 - 超大页码")
    void testPagePermissions_LargePageNumber() {
        Page<SysPermission> page = new Page<>(999999, 10);
        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        Page<SysPermission> result = permissionService.pagePermissions(
            page, tenantId, null, null, null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("分页查询 - 页大小为0")
    void testPagePermissions_ZeroPageSize() {
        Page<SysPermission> page = new Page<>(1, 0);
        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        Page<SysPermission> result = permissionService.pagePermissions(
            page, tenantId, null, null, null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("分页查询 - 超大页大小")
    void testPagePermissions_LargePageSize() {
        Page<SysPermission> page = new Page<>(1, 10000);
        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        Page<SysPermission> result = permissionService.pagePermissions(
            page, tenantId, null, null, null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("分页查询 - 空结果")
    void testPagePermissions_EmptyResult() {
        Page<SysPermission> page = new Page<>(1, 10);
        page.setRecords(Collections.emptyList());
        page.setTotal(0);
        
        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        Page<SysPermission> result = permissionService.pagePermissions(
            page, tenantId, "不存在的权限", null, null);

        assertNotNull(result);
        assertTrue(result.getRecords().isEmpty());
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("分页查询 - 超长搜索关键字")
    void testPagePermissions_LongSearchKeyword() {
        Page<SysPermission> page = new Page<>(1, 10);
        StringBuilder longKeyword = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longKeyword.append("测试");
        }
        
        when(permissionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(page);

        Page<SysPermission> result = permissionService.pagePermissions(
            page, tenantId, longKeyword.toString(), null, null);

        assertNotNull(result);
    }

    @Test
    @DisplayName("分页查询 - 特殊字符搜索")
    void testPageWrapper.class))).thenReturn(0L);
        when(permissionMapper.updateById(any(SysPermission.class))).thenReturn(1);

        assertDoesNotThrow(() -> permissionService.updatePermission(testPermission));
    }
}