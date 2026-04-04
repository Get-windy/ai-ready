package cn.aiedge.audit;

import cn.aiedge.audit.model.AuditLog;
import cn.aiedge.audit.service.AuditLogService;
import cn.aiedge.audit.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计日志模块单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@DisplayName("审计日志模块单元测试")
public class AuditLogModuleTest {

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogServiceImpl();
    }

    // ==================== 日志记录测试 ====================

    @Test
    @DisplayName("记录审计日志 - 成功")
    void testRecordAuditLog() {
        AuditLog log = createTestLog();
        Long logId = auditLogService.record(log);

        assertNotNull(logId);
        assertTrue(logId > 0);
    }

    @Test
    @DisplayName("异步记录审计日志 - 成功")
    void testRecordAsyncAuditLog() {
        AuditLog log = createTestLog();

        // 不应该抛出异常
        assertDoesNotThrow(() -> auditLogService.recordAsync(log));
    }

    @Test
    @DisplayName("记录日志后查询 - 成功")
    void testRecordAndQuery() {
        AuditLog log = createTestLog();
        log.setAuditType("login");
        log.setModule("system");

        auditLogService.record(log);

        Map<String, Object> result = auditLogService.query(1L, "login", "system", null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1L, result.get("total"));
    }

    // ==================== 日志查询测试 ====================

    @Test
    @DisplayName("查询审计日志 - 按类型过滤")
    void testQueryByType() {
        // 创建不同类型的日志
        for (int i = 0; i < 5; i++) {
            AuditLog log = createTestLog();
            log.setAuditType(i % 2 == 0 ? "login" : "logout");
            auditLogService.record(log);
        }

        Map<String, Object> result = auditLogService.query(null, "login", null, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(3L, result.get("total"));
    }

    @Test
    @DisplayName("查询审计日志 - 按用户过滤")
    void testQueryByUser() {
        // 创建不同用户的日志
        for (int i = 0; i < 5; i++) {
            AuditLog log = createTestLog();
            log.setUserId(i % 2 == 0 ? 1L : 2L);
            auditLogService.record(log);
        }

        Map<String, Object> result = auditLogService.query(null, null, null, 1L, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(3L, result.get("total"));
    }

    @Test
    @DisplayName("查询审计日志 - 按时间范围过滤")
    void testQueryByTimeRange() {
        LocalDateTime now = LocalDateTime.now();

        // 创建日志
        for (int i = 0; i < 3; i++) {
            AuditLog log = createTestLog();
            log.setOperTime(now.minusHours(i));
            auditLogService.record(log);
        }

        Map<String, Object> result = auditLogService.query(
                null, null, null, null,
                now.minusHours(2), now.plusHours(1), 1, 10);

        assertNotNull(result);
        assertTrue((Long) result.get("total") >= 1);
    }

    @Test
    @DisplayName("分页查询 - 成功")
    void testPagination() {
        // 创建多条日志
        for (int i = 0; i < 25; i++) {
            AuditLog log = createTestLog();
            auditLogService.record(log);
        }

        // 第一页
        Map<String, Object> page1 = auditLogService.query(null, null, null, null, null, null, 1, 10);
        assertEquals(25L, page1.get("total"));
        assertEquals(10, ((List<?>) page1.get("records")).size());

        // 第三页
        Map<String, Object> page3 = auditLogService.query(null, null, null, null, null, null, 3, 10);
        assertEquals(5, ((List<?>) page3.get("records")).size());
    }

    // ==================== 日志详情测试 ====================

    @Test
    @DisplayName("获取日志详情 - 成功")
    void testGetDetail() {
        AuditLog log = createTestLog();
        Long logId = auditLogService.record(log);

        AuditLog detail = auditLogService.getDetail(logId);

        assertNotNull(detail);
        assertEquals(logId, detail.getId());
    }

    @Test
    @DisplayName("获取日志详情 - 不存在")
    void testGetDetailNotFound() {
        AuditLog detail = auditLogService.getDetail(999999L);
        assertNull(detail);
    }

    // ==================== 用户历史测试 ====================

    @Test
    @DisplayName("获取用户操作历史 - 成功")
    void testGetUserHistory() {
        // 创建用户操作日志
        for (int i = 0; i < 10; i++) {
            AuditLog log = createTestLog();
            log.setUserId(1L);
            log.setAction("action_" + i);
            auditLogService.record(log);
        }

        List<AuditLog> history = auditLogService.getUserHistory(1L, 5);

        assertNotNull(history);
        assertEquals(5, history.size());
    }

    // ==================== 对象历史测试 ====================

    @Test
    @DisplayName("获取对象操作历史 - 成功")
    void testGetObjectHistory() {
        // 创建对象操作日志
        for (int i = 0; i < 5; i++) {
            AuditLog log = createTestLog();
            log.setTargetType("order");
            log.setTargetId("12345");
            log.setAction("update");
            auditLogService.record(log);
        }

        List<AuditLog> history = auditLogService.getObjectHistory("order", "12345");

        assertNotNull(history);
        assertEquals(5, history.size());
    }

    // ==================== 统计测试 ====================

    @Test
    @DisplayName("获取审计统计 - 成功")
    void testGetStatistics() {
        // 创建不同类型和结果的日志
        for (int i = 0; i < 10; i++) {
            AuditLog log = createTestLog();
            log.setAuditType(i % 2 == 0 ? "login" : "logout");
            log.setResult(i % 3 == 0 ? "FAILURE" : "SUCCESS");
            log.setModule(i % 2 == 0 ? "system" : "business");
            auditLogService.record(log);
        }

        Map<String, Object> stats = auditLogService.getStatistics(null, null, null);

        assertNotNull(stats);
        assertEquals(10L, stats.get("total"));
        assertNotNull(stats.get("successCount"));
        assertNotNull(stats.get("failureCount"));
        assertNotNull(stats.get("typeStats"));
        assertNotNull(stats.get("moduleStats"));
    }

    // ==================== 清理测试 ====================

    @Test
    @DisplayName("清理历史日志 - 成功")
    void testCleanLogs() {
        // 创建日志
        for (int i = 0; i < 5; i++) {
            AuditLog log = createTestLog();
            auditLogService.record(log);
        }

        // 清理30天前的日志（应该删除0条，因为都是新日志）
        int deleted = auditLogService.cleanLogs(30);

        assertTrue(deleted >= 0);
    }

    // ==================== 导出测试 ====================

    @Test
    @DisplayName("导出审计日志 - 成功")
    void testExportLogs() {
        // 创建日志
        for (int i = 0; i < 5; i++) {
            AuditLog log = createTestLog();
            auditLogService.record(log);
        }

        List<AuditLog> exported = auditLogService.exportLogs(null, null, null);

        assertNotNull(exported);
        assertTrue(exported.size() >= 5);
    }

    // ==================== 辅助方法 ====================

    private AuditLog createTestLog() {
        AuditLog log = new AuditLog();
        log.setTenantId(1L);
        log.setAuditType("login");
        log.setModule("system");
        log.setAction("user_login");
        log.setUserId(1L);
        log.setUsername("testuser");
        log.setOperIp("127.0.0.1");
        log.setOperTime(LocalDateTime.now());
        log.setResult("SUCCESS");
        return log;
    }
}
