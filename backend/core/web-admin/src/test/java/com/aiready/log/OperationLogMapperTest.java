package com.aiready.log;

import com.aiready.log.entity.OperationLog;
import com.aiready.log.mapper.OperationLogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 操作日志Mapper单元测试
 */
@SpringBootTest
@Transactional
public class OperationLogMapperTest {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 测试插入日志
     */
    @Test
    public void testInsert() {
        OperationLog log = createTestLog();
        
        int result = operationLogMapper.insert(log);
        
        assertEquals(1, result);
        assertNotNull(log.getId());
    }

    /**
     * 测试根据ID查询
     */
    @Test
    public void testSelectById() {
        OperationLog log = createTestLog();
        operationLogMapper.insert(log);
        
        OperationLog found = operationLogMapper.selectById(log.getId());
        
        assertNotNull(found);
        assertEquals(log.getModule(), found.getModule());
        assertEquals(log.getOperationType(), found.getOperationType());
    }

    /**
     * 测试更新日志
     */
    @Test
    public void testUpdate() {
        OperationLog log = createTestLog();
        operationLogMapper.insert(log);
        
        log.setOperationDesc("更新后的描述");
        int result = operationLogMapper.updateById(log);
        
        assertEquals(1, result);
        OperationLog updated = operationLogMapper.selectById(log.getId());
        assertEquals("更新后的描述", updated.getOperationDesc());
    }

    /**
     * 测试删除日志
     */
    @Test
    public void testDelete() {
        OperationLog log = createTestLog();
        operationLogMapper.insert(log);
        
        int result = operationLogMapper.deleteById(log.getId());
        
        assertEquals(1, result);
        assertNull(operationLogMapper.selectById(log.getId()));
    }

    /**
     * 测试批量插入日志
     */
    @Test
    public void testBatchInsert() {
        List<OperationLog> logs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            OperationLog log = createTestLog();
            log.setOperationDesc("批量插入测试" + i);
            logs.add(log);
        }
        
        int result = operationLogMapper.batchInsert(logs);
        
        assertEquals(5, result);
    }

    /**
     * 测试删除指定日期之前的日志
     */
    @Test
    public void testDeleteBeforeDate() {
        // 插入一条历史日志
        OperationLog oldLog = createTestLog();
        oldLog.setCreateTime(LocalDateTime.now().minusDays(30));
        operationLogMapper.insert(oldLog);
        
        int result = operationLogMapper.deleteBeforeDate(LocalDateTime.now().minusDays(1));
        
        assertTrue(result >= 0);
    }

    /**
     * 测试根据条件统计
     */
    @Test
    public void testCountByCondition() {
        // 插入测试数据
        OperationLog log1 = createTestLog();
        log1.setModule("统计测试");
        log1.setOperationType("CREATE");
        operationLogMapper.insert(log1);
        
        OperationLog log2 = createTestLog();
        log2.setModule("统计测试");
        log2.setOperationType("UPDATE");
        operationLogMapper.insert(log2);
        
        Long count = operationLogMapper.countByCondition(
                "统计测试", 
                "CREATE", 
                LocalDateTime.now().minusDays(1), 
                LocalDateTime.now().plusDays(1));
        
        assertNotNull(count);
        assertTrue(count >= 1);
    }

    /**
     * 创建测试日志对象
     */
    private OperationLog createTestLog() {
        OperationLog log = new OperationLog();
        log.setModule("测试模块");
        log.setOperationType("CREATE");
        log.setOperationDesc("创建测试数据");
        log.setRequestMethod("POST");
        log.setRequestUrl("/api/test/create");
        log.setOperatorIp("127.0.0.1");
        log.setStatus(1);
        log.setExecutionTime(100L);
        log.setCreateTime(LocalDateTime.now());
        return log;
    }
}
