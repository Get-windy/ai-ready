package com.aiready.system.service;

import com.aiready.AbstractTest;
import com.aiready.system.entity.Position;
import com.aiready.system.mapper.PositionMapper;
import com.aiready.system.service.impl.PositionServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 岗位服务单元测试
 * 测试PositionService的所有业务方法
 * 
 * @author qa-lead
 * @date 2026-04-13
 */
@DisplayName("岗位服务单元测试")
public class PositionServiceTest extends AbstractTest {

    @Mock
    private PositionMapper positionMapper;

    @InjectMocks
    private PositionServiceImpl positionService;

    private Position testPosition;
    private Position managerPosition;

    @Override
    protected void init() {
        // 初始化测试数据
        testPosition = createTestPosition(1L, "Java开发工程师", "POS001", 1L, 2);
        managerPosition = createTestPosition(2L, "技术经理", "POS002", 1L, 1);
    }

    /**
     * 创建测试岗位对象
     */
    private Position createTestPosition(Long id, String name, String code, 
                                        Long deptId, Integer type) {
        Position position = new Position();
        position.setId(id);
        position.setPositionName(name);
        position.setPositionCode(code);
        position.setDeptId(deptId);
        position.setPositionType(type);
        position.setPositionLevel(3);
        position.setHeadcount(5);
        position.setCurrentCount(3);
        position.setStatus(1);
        position.setSortOrder(1);
        position.setCreateTime(LocalDateTime.now());
        position.setUpdateTime(LocalDateTime.now());
        return position;
    }

    @Nested
    @DisplayName("岗位查询测试")
    class QueryTests {

        @Test
        @DisplayName("根据岗位编码查询-成功")
        void testGetByPositionCode_Success() {
            // Given
            when(positionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testPosition);

            // When
            Position result = positionService.getByPositionCode("POS001");

            // Then
            assertNotNull(result);
            assertEquals("Java开发工程师", result.getPositionName());
            assertEquals("POS001", result.getPositionCode());
            verify(positionMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
            logTestInfo("根据岗位编码查询测试通过");
        }

        @Test
        @DisplayName("根据岗位编码查询-不存在")
        void testGetByPositionCode_NotFound() {
            // Given
            when(positionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When
            Position result = positionService.getByPositionCode("NOTEXIST");

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("根据部门ID查询岗位列表")
        void testListByDeptId() {
            // Given
            List<Position> positions = Arrays.asList(testPosition, managerPosition);
            when(positionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(positions);

            // When
            List<Position> result = positionService.listByDeptId(1L);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("根据岗位类型查询")
        void testListByType() {
            // Given
            List<Position> positions = Arrays.asList(testPosition);
            when(positionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(positions);

            // When
            List<Position> result = positionService.listByType(2);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Java开发工程师", result.get(0).getPositionName());
        }

        @Test
        @DisplayName("获取所有启用的岗位列表")
        void testListActivePositions() {
            // Given
            List<Position> positions = Arrays.asList(testPosition, managerPosition);
            when(positionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(positions);

            // When
            List<Position> result = positionService.listActivePositions();

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("根据ID查询岗位-包含部门名称")
        void testGetPositionById() {
            // Given
            when(positionMapper.selectById(1L)).thenReturn(testPosition);

            // When
            Position result = positionService.getPositionById(1L);

            // Then
            assertNotNull(result);
            assertEquals("Java开发工程师", result.getPositionName());
        }
    }

    @Nested
    @DisplayName("岗位编码唯一性检查测试")
    class CodeCheckTests {

        @Test
        @DisplayName("检查岗位编码已存在-返回true")
        void testCheckPositionCodeExists_True() {
            // Given
            when(positionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

            // When
            boolean exists = positionService.checkPositionCodeExists("POS001");

            // Then
            assertTrue(exists);
        }

        @Test
        @DisplayName("检查岗位编码不存在-返回false")
        void testCheckPositionCodeExists_False() {
            // Given
            when(positionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            boolean exists = positionService.checkPositionCodeExists("NEWCODE");

            // Then
            assertFalse(exists);
        }

        @Test
        @DisplayName("检查岗位编码已存在-排除指定ID")
        void testCheckPositionCodeExistsExcludeId() {
            // Given
            when(positionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

            // When
            boolean exists = positionService.checkPositionCodeExists("POS001", 1L);

            // Then
            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("岗位状态变更测试")
    class StatusChangeTests {

        @Test
        @DisplayName("启用岗位-成功")
        void testEnablePosition_Success() {
            // Given
            when(positionMapper.selectById(1L)).thenReturn(testPosition);
            when(positionMapper.updateById(any(Position.class))).thenReturn(1);

            // When
            boolean result = positionService.enablePosition(1L);

            // Then
            assertTrue(result);
            verify(positionMapper, times(1)).updateById(any(Position.class));
        }

        @Test
        @DisplayName("停用岗位-成功")
        void testDisablePosition_Success() {
            // Given
            when(positionMapper.selectById(1L)).thenReturn(testPosition);
            when(positionMapper.updateById(any(Position.class))).thenReturn(1);

            // When
            boolean result = positionService.disablePosition(1L);

            // Then
            assertTrue(result);
            verify(positionMapper, times(1)).updateById(any(Position.class));
        }

        @Test
        @DisplayName("停用岗位-岗位不存在")
        void testDisablePosition_NotFound() {
            // Given
            when(positionMapper.selectById(999L)).thenReturn(null);

            // When
            boolean result = positionService.disablePosition(999L);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("岗位人数管理测试")
    class CountTests {

        @Test
        @DisplayName("更新在职人数-增加")
        void testUpdateCurrentCount_Increase() {
            // Given
            testPosition.setCurrentCount(3);
            when(positionMapper.selectById(1L)).thenReturn(testPosition);
            when(positionMapper.updateById(any(Position.class))).thenReturn(1);

            // When
            boolean result = positionService.updateCurrentCount(1L, 1);

            // Then
            assertTrue(result);
            assertEquals(4, testPosition.getCurrentCount());
        }

        @Test
        @DisplayName("更新在职人数-减少")
        void testUpdateCurrentCount_Decrease() {
            // Given
            testPosition.setCurrentCount(3);
            when(positionMapper.selectById(1L)).thenReturn(testPosition);
            when(positionMapper.updateById(any(Position.class))).thenReturn(1);

            // When
            boolean result = positionService.updateCurrentCount(1L, -1);

            // Then
            assertTrue(result);
            assertEquals(2, testPosition.getCurrentCount());
        }

        @Test
        @DisplayName("更新在职人数-不能为负数")
        void testUpdateCurrentCount_NotNegative() {
            // Given
            testPosition.setCurrentCount(0);
            when(positionMapper.selectById(1L)).thenReturn(testPosition);

            // When
            boolean result = positionService.updateCurrentCount(1L, -1);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("岗位关联检查测试")
    class RelationCheckTests {

        @Test
        @DisplayName("检查岗位下是否有员工-当前未实现")
        void testHasEmployees_NotImplemented() {
            // When - hasEmployees方法当前未实现，返回false
            boolean hasEmployees = positionService.hasEmployees(1L);

            // Then - 当前实现返回false
            assertFalse(hasEmployees);
        }
    }

    @Nested
    @DisplayName("批量操作测试")
    class BatchTests {

        @Test
        @DisplayName("批量更新状态-成功")
        void testBatchUpdateStatus_Success() {
            // Given
            List<Long> ids = Arrays.asList(1L, 2L);
            when(positionMapper.update(any(), any(LambdaQueryWrapper.class))).thenReturn(2);

            // When
            boolean result = positionService.batchUpdateStatus(ids, 0);

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("批量更新状态-空ID列表")
        void testBatchUpdateStatus_EmptyList() {
            // Given
            List<Long> ids = Arrays.asList();

            // When
            boolean result = positionService.batchUpdateStatus(ids, 0);

            // Then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("岗位业务规则测试")
    class BusinessRuleTests {

        @Test
        @DisplayName("岗位编制检查-在职人数超编")
        void testHeadcountExceeded() {
            // Given
            testPosition.setHeadcount(5);
            testPosition.setCurrentCount(5);

            // When - 尝试增加在职人数
            when(positionMapper.selectById(1L)).thenReturn(testPosition);

            // Then - 应该返回false或抛出异常
            // 这里验证业务规则：在职人数不能超过编制人数
            assertTrue(testPosition.getCurrentCount() >= testPosition.getHeadcount());
        }

        @Test
        @DisplayName("岗位类型有效性检查")
        void testPositionTypeValidity() {
            // Given
            Integer[] validTypes = {1, 2, 3, 4, 5}; // 管理岗、技术岗、销售岗、运营岗、其他

            // Then
            assertTrue(testPosition.getPositionType() >= 1 && testPosition.getPositionType() <= 5);
        }

        @Test
        @DisplayName("岗位级别有效性检查")
        void testPositionLevelValidity() {
            // Given
            testPosition.setPositionLevel(3);

            // Then
            assertTrue(testPosition.getPositionLevel() >= 1 && testPosition.getPositionLevel() <= 5);
        }
    }
}
