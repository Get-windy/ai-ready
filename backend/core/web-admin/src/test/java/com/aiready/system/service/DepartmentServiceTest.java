package com.aiready.system.service;

import com.aiready.AbstractTest;
import com.aiready.system.entity.Department;
import com.aiready.system.mapper.DepartmentMapper;
import com.aiready.system.service.impl.DepartmentServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 部门服务单元测试
 * 测试DepartmentService的所有业务方法
 * 
 * @author qa-lead
 * @date 2026-04-13
 */
@DisplayName("部门服务单元测试")
public class DepartmentServiceTest extends AbstractTest {

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department testDept;
    private Department childDept;

    @Override
    protected void init() {
        // 初始化测试数据
        testDept = createTestDepartment(1L, "技术部", "DEPT001", 0L);
        childDept = createTestDepartment(2L, "研发组", "DEPT002", 1L);
    }

    /**
     * 创建测试部门对象
     */
    private Department createTestDepartment(Long id, String name, String code, Long parentId) {
        Department dept = new Department();
        dept.setId(id);
        dept.setDeptName(name);
        dept.setDeptCode(code);
        dept.setParentId(parentId);
        if (parentId == 0L) {
            dept.setAncestors("0,");
        } else {
            dept.setAncestors("0,1,");
        }
        dept.setStatus(1);
        dept.setDeptType(2);
        dept.setSortOrder(1);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        return dept;
    }

    @Nested
    @DisplayName("部门查询测试")
    class QueryTests {

        @Test
        @DisplayName("根据部门编码查询-成功")
        void testGetByDeptCode_Success() {
            // Given
            when(departmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testDept);

            // When
            Department result = departmentService.getByDeptCode("DEPT001");

            // Then
            assertNotNull(result);
            assertEquals("技术部", result.getDeptName());
            assertEquals("DEPT001", result.getDeptCode());
            verify(departmentMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
            logTestInfo("根据部门编码查询测试通过");
        }

        @Test
        @DisplayName("根据部门编码查询-不存在")
        void testGetByDeptCode_NotFound() {
            // Given
            when(departmentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When
            Department result = departmentService.getByDeptCode("NOTEXIST");

            // Then
            assertNull(result);
            verify(departmentMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("根据父部门ID查询子部门列表")
        void testListByParentId() {
            // Given
            List<Department> childDepts = Arrays.asList(childDept);
            when(departmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(childDepts);

            // When
            List<Department> result = departmentService.listByParentId(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("研发组", result.get(0).getDeptName());
        }

        @Test
        @DisplayName("根据ID查询部门-包含父部门名称")
        void testGetDeptById() {
            // Given
            when(departmentMapper.selectById(1L)).thenReturn(testDept);
            when(departmentMapper.selectById(0L)).thenReturn(null);

            // When
            Department result = departmentService.getDeptById(1L);

            // Then
            assertNotNull(result);
            assertEquals("技术部", result.getDeptName());
        }
    }

    @Nested
    @DisplayName("部门编码唯一性检查测试")
    class CodeCheckTests {

        @Test
        @DisplayName("检查部门编码已存在-返回true")
        void testCheckDeptCodeExists_True() {
            // Given
            when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // When
            boolean exists = departmentService.checkDeptCodeExists("DEPT001");

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("检查部门编码不存在-返回false")
        void testCheckDeptCodeExists_False() {
            // Given
            when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            boolean exists = departmentService.checkDeptCodeExists("NEWCODE");

            // Then
            assertFalse(exists);
        }

        @Test
        @DisplayName("检查部门编码已存在-排除指定ID")
        void testCheckDeptCodeExistsExcludeId() {
            // Given
            when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            boolean exists = departmentService.checkDeptCodeExists("DEPT001", 1L);

            // Then
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("部门状态变更测试")
    class StatusChangeTests {

        @Test
        @DisplayName("启用部门-成功")
        void testEnableDept_Success() {
            // Given
            when(departmentMapper.selectById(1L)).thenReturn(testDept);
            when(departmentMapper.updateById(any(Department.class))).thenReturn(1);

            // When
            boolean result = departmentService.enableDept(1L);

            // Then
            assertTrue(result);
            verify(departmentMapper, times(1)).updateById(any(Department.class));
        }

        @Test
        @DisplayName("停用部门-成功")
        void testDisableDept_Success() {
            // Given
            when(departmentMapper.selectById(1L)).thenReturn(testDept);
            when(departmentMapper.updateById(any(Department.class))).thenReturn(1);

            // When
            boolean result = departmentService.disableDept(1L);

            // Then
            assertTrue(result);
            verify(departmentMapper, times(1)).updateById(any(Department.class));
        }

        @Test
        @DisplayName("启用部门-部门不存在")
        void testEnableDept_NotFound() {
            // Given
            when(departmentMapper.selectById(999L)).thenReturn(null);

            // When
            boolean result = departmentService.enableDept(999L);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("部门关联检查测试")
    class RelationCheckTests {

        @Test
        @DisplayName("检查部门是否有子部门-有子部门")
        void testHasChildren_True() {
            // Given
            when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // When
            boolean hasChildren = departmentService.hasChildren(1L);

            // Then
            assertTrue(hasChildren);
        }

        @Test
        @DisplayName("检查部门是否有子部门-无子部门")
        void testHasChildren_False() {
            // Given
            when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            boolean hasChildren = departmentService.hasChildren(1L);

            // Then
            assertFalse(hasChildren);
        }

        @Test
        @DisplayName("检查部门下是否有用户-当前未实现")
        void testHasUsers_NotImplemented() {
            // When - hasUsers方法当前未实现，返回false
            boolean hasUsers = departmentService.hasUsers(1L);

            // Then - 当前实现返回false
            assertFalse(hasUsers);
        }
    }

    @Nested
    @DisplayName("部门树形结构测试")
    class TreeTests {

        @Test
        @DisplayName("获取部门树形结构")
        void testGetDeptTree() {
            // Given
            List<Department> allDepts = Arrays.asList(testDept, childDept);
            when(departmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(allDepts);

            // When
            List<Department> tree = departmentService.getDeptTree();

            // Then
            assertNotNull(tree);
            assertFalse(tree.isEmpty());
        }

        @Test
        @DisplayName("获取所有子部门ID")
        void testGetChildDeptIds() {
            // Given
            List<Department> childDepts = Arrays.asList(childDept);
            when(departmentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(childDepts);

            // When
            List<Long> childIds = departmentService.getChildDeptIds(1L);

            // Then
            assertNotNull(childIds);
            assertTrue(childIds.contains(2L));
        }
    }

    @Nested
    @DisplayName("部门移动测试")
    class MoveTests {

        @Test
        @DisplayName("移动部门-成功")
        void testMoveDept_Success() {
            // Given
            Department parentDept = createTestDepartment(3L, "新父部门", "DEPT003", 0L);
            when(departmentMapper.selectById(2L)).thenReturn(childDept);
            when(departmentMapper.selectById(3L)).thenReturn(parentDept);
            when(departmentMapper.updateById(any(Department.class))).thenReturn(1);

            // When
            boolean result = departmentService.moveDept(2L, 3L);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("移动部门-目标部门不存在")
        void testMoveDept_ParentNotFound() {
            // Given
            when(departmentMapper.selectById(2L)).thenReturn(childDept);
            when(departmentMapper.selectById(999L)).thenReturn(null);

            // When
            boolean result = departmentService.moveDept(2L, 999L);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("部门层级测试")
    class LevelTests {

        @Test
        @DisplayName("获取部门层级-根部门")
        void testGetDeptLevel_Root() {
            // Given - testDept 已经在 init() 中设置了 ancestors = "0,"
            when(departmentMapper.selectById(1L)).thenReturn(testDept);

            // When
            Integer level = departmentService.getDeptLevel(1L);

            // Then
            assertEquals(1, level);
        }

        @Test
        @DisplayName("获取部门层级-子部门")
        void testGetDeptLevel_Child() {
            // Given - childDept 已经在 init() 中设置了 ancestors = "0,1,"
            when(departmentMapper.selectById(2L)).thenReturn(childDept);

            // When
            Integer level = departmentService.getDeptLevel(2L);

            // Then
            assertEquals(2, level);
        }
    }
}
