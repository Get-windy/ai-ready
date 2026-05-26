package cn.aiedge.base.log;

import cn.aiedge.base.entity.SysOperLog;
import cn.aiedge.base.mapper.SysOperLogMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 日志查询模块单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("日志查询模块单元测试")
public class LogQueryModuleTest {

    @Mock
    private SysOperLogMapper operLogMapper;

    @InjectMocks
    private AdvancedLogQueryServiceImpl logQueryService;

    private List<SysOperLog> testLogs;

    @BeforeEach
    void setUp() {
        testLogs = createTestLogs();
    }

    // ==================== 高级查询测试 ====================

    @Test
    @DisplayName("高级查询 - 基础查询")
    void testBasicQuery() {
        // 准备数据
        Page<SysOperLog> mockPage = new Page<>(1, 20);
        mockPage.setRecords(testLogs);
        mockPage.setTotal(testLogs.size());

        when(operLogMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(operLogMapper.selectList(any())).thenReturn(testLogs);

        // 执行查询
        LogQueryRequest request = new LogQueryRequest();
        request.setPage(1);
        request.setPageSize(20);

        LogQueryResponse response = logQueryService.query(request);

        // 验证结果
        assertNotNull(response);
        assertEquals(20, response.getPageSize());
        assertNotNull(response.getAggregation());
    }

    @Test
    @DisplayName("高级查询 - 按用户过滤")
    void testQueryByUser() {
        // 准备数据
        Page<SysOperLog> mockPage = new Page<>(1, 20);
        List<SysOperLog> userLogs = testLogs.stream()
                .filter(l -> l.getUserId().equals(1L))
                .toList();
        mockPage.setRecords(userLogs);
        mockPage.setTotal(userLogs.size());

        when(operLogMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(operLogMapper.selectList(any())).thenReturn(userLogs);

        // 执行查询
        LogQueryRequest request = new LogQueryRequest();
        request.setUserId(1L);

        LogQueryResponse response = logQueryService.query(request);

        // 验证结果
        assertNotNull(response);
        assertTrue(response.getRecords().stream()
                .allMatch(l -> ((SysOperLog) l).getUserId().equals(1L)));
    }

    @Test
    @DisplayName("高级查询 - 按时间范围过滤")
    void testQueryByTimeRange() {
        // 准备数据
        LocalDateTime startTime = LocalDateTime.now().minusDays(1);
        LocalDateTime endTime = LocalDateTime.now();

        Page<SysOperLog> mockPage = new Page<>(1, 20);
        mockPage.setRecords(testLogs);
        mockPage.setTotal(testLogs.size());

        when(operLogMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(operLogMapper.selectList(any())).thenReturn(testLogs);

        // 执行查询
        LogQueryRequest request = new LogQueryRequest();
        request.setStartTime(startTime);
        request.setEndTime(endTime);

        LogQueryResponse response = logQueryService.query(request);

        // 验证结果
        assertNotNull(response);
    }

    @Test
    @DisplayName("高级查询 - 按状态过滤")
    void testQueryByStatus() {
        // 准备数据
        Page<SysOperLog> mockPage = new Page<>(1, 20);
        List<SysOperLog> successLogs = testLogs.stream()
                .filter(l -> l.getStatus() == 0)
                .toList();
        mockPage.setRecords(successLogs);
        mockPage.setTotal(successLogs.size());

        when(operLogMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(operLogMapper.selectList(any())).thenReturn(successLogs);

        // 执行查询
        LogQueryRequest request = new LogQueryRequest();
        request.setStatus(0); // 成功

        LogQueryResponse response = logQueryService.query(request);

        // 验证结果
        assertNotNull(response);
        assertTrue(response.getRecords().stream()
                .allMatch(l -> ((SysOperLog) l).getStatus() == 0));
    }

    @Test
    @DisplayName("高级查询 - 按耗时范围过滤")
    void testQueryByCostTime() {
        // 准备数据
        Page<SysOperLog> mockPage = new Page<>(1, 20);
        List<SysOperLog> slowLogs = testLogs.stream()
                .filter(l -> l.getCostTime() >= 1000 && l.getCostTime() <= 5000)
                .toList();
        mockPage.setRecords(slowLogs);
        mockPage.setTotal(slowLogs.size());

        when(operLogMapper.selectPage(any(), any())).thenReturn(mockPage);
        when(operLogMapper.selectList(any())).thenReturn(slowLogs);

        // 执行查询
        LogQueryRequest request = new LogQueryRequest();
        request.setMinCostTime(1000L);
        request.setMaxCostTime(5000L);

        LogQueryResponse response = logQueryService.query(request);

        // 验证结果
        assertNotNull(response);
    }

    // ==================== 全文检索测试 ====================

    @Test
    @DisplayName("全文检索 - 关键词搜索")
    void testFullTextSearch() {
        // 准备数据
        Page<SysOperLog> mockPage = new Page<>(1, 20);
        mockPage.setRecords(testLogs);
        mockPage.setTotal(testLogs.size());

        when(operLogMapper.selectPage(any(), any())).thenReturn(mockPage);

        // 执行查询
        Page<SysOperLog> result = logQueryService.fullTextSearch("login", 1, 20);

        // 验证结果
        assertNotNull(result);
        verify(operLogMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("全文检索 - 空关键词")
    void testFullTextSearchEmpty() {
        // 执行查询
        Page<SysOperLog> result = logQueryService.fullTextSearch("", 1, 20);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
    }

    // ==================== IP查询测试 ====================

    @Test
    @DisplayName("IP查询 - 成功")
    void testQueryByIp() {
        // 准备数据
        when(operLogMapper.selectList(any())).thenReturn(testLogs);

        // 执行查询
        List<SysOperLog> result = logQueryService.queryByIp("192.168", null, null);

        // 验证结果
        assertNotNull(result);
    }

    // ==================== 慢操作查询测试 ====================

    @Test
    @DisplayName("慢操作查询 - 成功")
    void testQuerySlowOperations() {
        // 准备数据
        List<SysOperLog> slowLogs = testLogs.stream()
                .filter(l -> l.getCostTime() >= 1000)
                .toList();
        when(operLogMapper.selectList(any())).thenReturn(slowLogs);

        // 执行查询
        List<SysOperLog> result = logQueryService.querySlowOperations(1000L, null, 100);

        // 验证结果
        assertNotNull(result);
    }

    // ==================== 失败操作查询测试 ====================

    @Test
    @DisplayName("失败操作查询 - 成功")
    void testQueryFailedOperations() {
        // 准备数据
        List<SysOperLog> failedLogs = testLogs.stream()
                .filter(l -> l.getStatus() == 1)
                .toList();
        when(operLogMapper.selectList(any())).thenReturn(failedLogs);

        // 执行查询
        List<SysOperLog> result = logQueryService.queryFailedOperations(null, null, 100);

        // 验证结果
        assertNotNull(result);
    }

    // ==================== 统计摘要测试 ====================

    @Test
    @DisplayName("统计摘要 - 成功")
    void testGetLogSummary() {
        // 准备数据
        when(operLogMapper.selectList(any())).thenReturn(testLogs);

        // 执行查询
        Map<String, Object> summary = logQueryService.getLogSummary(null, null);

        // 验证结果
        assertNotNull(summary);
        assertTrue(summary.containsKey("totalOperations"));
        assertTrue(summary.containsKey("successCount"));
        assertTrue(summary.containsKey("failCount"));
        assertTrue(summary.containsKey("moduleDistribution"));
    }

    // ==================== 操作趋势测试 ====================

    @Test
    @DisplayName("操作趋势 - 按天统计")
    void testGetOperationTrendByDay() {
        // 准备数据
        when(operLogMapper.selectList(any())).thenReturn(testLogs);

        // 执行查询
        List<Map<String, Object>> trend = logQueryService.getOperationTrend(null, null, "day");

        // 验证结果
        assertNotNull(trend);
    }

    @Test
    @DisplayName("操作趋势 - 按小时统计")
    void testGetOperationTrendByHour() {
        // 准备数据
        when(operLogMapper.selectList(any())).thenReturn(testLogs);

        // 执行查询
        List<Map<String, Object>> trend = logQueryService.getOperationTrend(null, null, "hour");

        // 验证结果
        assertNotNull(trend);
    }

    // ==================== 用户活动分析测试 ====================

    @Test
    @DisplayName("用户活动分析 - 成功")
    void testGetUserActivityAnalysis() {
        // 准备数据
        List<SysOperLog> userLogs = testLogs.stream()
                .filter(l -> l.getUserId().equals(1L))
                .toList();
        when(operLogMapper.selectList(any())).thenReturn(userLogs);

        // 执行查询
        Map<String, Object> analysis = logQueryService.getUserActivityAnalysis(1L, 7);

        // 验证结果
        assertNotNull(analysis);
        assertEquals(1L, analysis.get("userId"));
        assertTrue(analysis.containsKey("totalOperations"));
        assertTrue(analysis.containsKey("moduleStats"));
    }

    // ==================== 异常检测测试 ====================

    @Test
    @DisplayName("异常检测 - 频繁失败")
    void testDetectAnomalousOperations() {
        // 准备数据 - 模拟大量失败
        when(operLogMapper.selectCount(any())).thenReturn(15L);

        // 执行查询
        List<Map<String, Object>> anomalies = logQueryService.detectAnomalousOperations(null);

        // 验证结果
        assertNotNull(anomalies);
    }

    // ==================== 导出测试 ====================

    @Test
    @DisplayName("导出服务 - CSV格式")
    void testExportToCsv() throws Exception {
        LogExportService exportService = new LogExportService();
        
        // 模拟响应
        jakarta.servlet.http.HttpServletResponse mockResponse = 
                mock(jakarta.servlet.http.HttpServletResponse.class);
        jakarta.servlet.ServletOutputStream mockOutputStream = 
                mock(jakarta.servlet.ServletOutputStream.class);
        when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);

        // 执行导出
        exportService.exportToCsv(testLogs, mockResponse);

        // 验证
        verify(mockResponse).setContentType("text/csv;charset=UTF-8");
    }

    @Test
    @DisplayName("导出服务 - JSON格式")
    void testExportToJson() throws Exception {
        LogExportService exportService = new LogExportService();
        
        // 模拟响应
        jakarta.servlet.http.HttpServletResponse mockResponse = 
                mock(jakarta.servlet.http.HttpServletResponse.class);
        jakarta.servlet.ServletOutputStream mockOutputStream = 
                mock(jakarta.servlet.ServletOutputStream.class);
        when(mockResponse.getOutputStream()).thenReturn(mockOutputStream);

        // 执行导出
        exportService.exportToJson(testLogs, mockResponse);

        // 验证
        verify(mockResponse).setContentType("application/json;charset=UTF-8");
    }

    // ==================== 辅助方法 ====================

    private List<SysOperLog> createTestLogs() {
        List<SysOperLog> logs = new ArrayList<>();

        // 成功日志
        for (int i = 0; i < 5; i++) {
            SysOperLog log = new SysOperLog();
            log.setId((long) i);
            log.setTenantId(1L);
            log.setUserId(1L);
            log.setUsername("user1");
            log.setModule("system");
            log.setAction("login");
            log.setRequestMethod("POST");
            log.setRequestUrl("/api/auth/login");
            log.setStatus(0); // 成功
            log.setCostTime(100L + i * 50);
            log.setOperTime(LocalDateTime.now().minusHours(i));
            log.setOperIp("192.168.1." + i);
            log.setOperLocation("北京");
            logs.add(log);
        }

        // 失败日志
        for (int i = 5; i < 8; i++) {
            SysOperLog log = new SysOperLog();
            log.setId((long) i);
            log.setTenantId(1L);
            log.setUserId(2L);
            log.setUsername("user2");
            log.setModule("order");
            log.setAction("create");
            log.setRequestMethod("POST");
            log.setRequestUrl("/api/order/create");
            log.setStatus(1); // 失败
            log.setCostTime(2000L + i * 100);
            log.setOperTime(LocalDateTime.now().minusHours(i - 5));
            log.setOperIp("10.0.0." + i);
            log.setOperLocation("上海");
            log.setErrorMsg("库存不足");
            logs.add(log);
        }

        // 慢操作日志
        for (int i = 8; i < 10; i++) {
            SysOperLog log = new SysOperLog();
            log.setId((long) i);
            log.setTenantId(1L);
            log.setUserId(1L);
            log.setUsername("user1");
            log.setModule("report");
            log.setAction("export");
            log.setRequestMethod("GET");
            log.setRequestUrl("/api/report/export");
            log.setStatus(0);
            log.setCostTime(5000L + i * 1000);
            log.setOperTime(LocalDateTime.now().minusMinutes(i));
            log.setOperIp("192.168.1." + i);
            log.setOperLocation("北京");
            logs.add(log);
        }

        return logs;
    }
}
