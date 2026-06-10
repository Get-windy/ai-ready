package cn.aiedge.erp.printing.service;

import cn.aiedge.common.cache.RedisCache;
import cn.aiedge.erp.printing.dto.v2.*;
import cn.aiedge.erp.printing.entity.v2.*;
import cn.aiedge.erp.printing.engine.ExpressionEvaluator;
import cn.aiedge.erp.printing.engine.FormatEngine;
import cn.aiedge.erp.printing.engine.FormatEngineImpl;
import cn.aiedge.erp.printing.mapper.*;
import cn.aiedge.erp.printing.mq.PrintTaskProducer;
import cn.aiedge.erp.printing.service.impl.ChainExecutorServiceImpl;
import cn.aiedge.erp.printing.service.impl.PrintChainServiceImpl;
import cn.aiedge.erp.printing.service.impl.PrintClientServiceImpl;
import cn.aiedge.erp.printing.service.impl.ScreenshotServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 打印链路与服务集成测试
 * 使用 Mock 隔离数据库层，验证业务逻辑正确性
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("打印链路与任务服务测试")
class ChainServiceTest {

    @Mock
    private SysPrintChainMapper chainMapper;
    @Mock
    private SysPrintChainItemMapper chainItemMapper;
    @Mock
    private SysPrintTemplateMapper templateMapper;
    @Mock
    private SysPrintClientMapper clientMapper;
    @Mock
    private SysPrintTaskMapper taskMapper;
    @Mock
    private SysScreenshotTaskMapper screenshotMapper;
    @Mock
    private PrintTaskProducer taskProducer;
    @Mock
    private RedisCache redisCache;

    private PrintChainService chainService;
    private PrintClientService clientService;
    private ScreenshotService screenshotService;
    private ChainExecutorService executorService;
    private FormatEngine formatEngine;

    @BeforeEach
    void setUp() {
        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        ObjectMapper objectMapper = new ObjectMapper();
        formatEngine = new FormatEngineImpl(evaluator, objectMapper);

        chainService = new PrintChainServiceImpl(chainMapper, chainItemMapper, templateMapper, clientMapper);
        clientService = new PrintClientServiceImpl(clientMapper);
        screenshotService = new ScreenshotServiceImpl(
                screenshotMapper, templateMapper, formatEngine, objectMapper, redisCache
        );
        executorService = new ChainExecutorServiceImpl(
                chainMapper, chainItemMapper, taskMapper, templateMapper,
                clientMapper, screenshotMapper, screenshotService,
                objectMapper, taskProducer, redisCache
        );
    }

    @Test
    @DisplayName("链路创建 - 重名校验")
    void testChainCreateDuplicateName() {
        // 模拟已存在同名链路
        when(chainMapper.selectCount(any())).thenReturn(1L);

        PrintChainCreateRequest request = new PrintChainCreateRequest();
        request.setPageCode("sale_order");
        request.setChainName("销售单打印");
        request.setItems(List.of());

        assertThrows(RuntimeException.class, () ->
                chainService.createChain(request, 1L, 1L)
        );
    }

    @Test
    @DisplayName("链路列表查询")
    void testChainList() {
        // 模拟分页查询
        Page<SysPrintChain> pageResult = new Page<>(1, 20, 0);
        when(chainMapper.selectPage(any(), any())).thenReturn(pageResult);

        Page<PrintChainVO> result = chainService.listChains(1, 20, null, 1L);
        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    @DisplayName("客户端注册 - 重名校验")
    void testClientRegisterDuplicate() {
        when(clientMapper.selectCount(any())).thenReturn(1L);

        PrintClientRegisterRequest request = new PrintClientRegisterRequest();
        request.setClientName("仓库打印机");

        assertThrows(RuntimeException.class, () ->
                clientService.register(request, 1L, 1L)
        );
    }

    @Test
    @DisplayName("格式化引擎 - validateFormula")
    void testValidateFormula() {
        // 直接的 FormatEngine 测试，不涉及数据库
        FormulaValidationResult result = formatEngine.validateExpression(
                "'¥' + value + '元'", 99.9
        );
        assertTrue(result.isValid());
        assertEquals("¥99.9元", result.getPreviewResult());
    }

    @Test
    @DisplayName("截图任务创建 - 模板不存在")
    void testScreenshotTemplateNotFound() {
        when(templateMapper.selectById(any())).thenReturn(null);

        ScreenshotCreateRequest request = new ScreenshotCreateRequest();
        request.setTemplateId(999L);
        request.setDataJson(java.util.Map.of("test", "data"));

        assertThrows(RuntimeException.class, () ->
                screenshotService.createScreenshotTask(request, 1L, 1L)
        );
    }

    @Test
    @DisplayName("格式化引擎 - 复杂表达式组合")
    void testComplexExpression() {
        // 测试：value === '已审核' ? '通过' : value === '待审核' ? '待定' : '拒绝'
        String expr = "value == '已审核' ? '通过' : (value == '待审核' ? '待定' : '拒绝')";

        FormulaValidationResult r1 = formatEngine.validateExpression(expr, "已审核");
        assertTrue(r1.isValid());
        assertEquals("通过", r1.getPreviewResult());

        FormulaValidationResult r2 = formatEngine.validateExpression(expr, "待审核");
        assertTrue(r2.isValid());
        assertEquals("待定", r2.getPreviewResult());

        FormulaValidationResult r3 = formatEngine.validateExpression(expr, "未知");
        assertTrue(r3.isValid());
        assertEquals("拒绝", r3.getPreviewResult());
    }

    @Test
    @DisplayName("Redis 锁 - 锁被占用时跳过分发")
    void testDispatchToClientLockOccupied() {
        // 模拟锁已被其他任务占用
        when(redisCache.setIfAbsent(
                eq("print:client:lock:1"), anyString(), eq(30L), eq(TimeUnit.SECONDS)
        )).thenReturn(false);
        when(redisCache.get("print:client:lock:1")).thenReturn("OTHER_TASK");

        // 创建任务并尝试执行 (通过 executeStep 触发 dispatchToClient)
        SysPrintTask task = new SysPrintTask();
        task.setTaskId(1L);
        task.setClientId(1L);
        task.setTaskCode("TEST001");

        // executeStep 会调用 taskMapper.selectById → processTask → dispatchToClient
        // dispatchToClient 检查锁，锁被占用则标记为 QUEUED
        when(taskMapper.selectById(1L)).thenReturn(task);

        executorService.executeStep(1L);

        // 验证状态被标记为 QUEUED 而非 PRINTING
        verify(taskMapper).updateById(argThat(t ->
                "QUEUED".equals(t.getStatus())
        ));
        verify(taskProducer, never()).sendTaskToClient(any(), anyLong());
    }

    @Test
    @DisplayName("Redis 锁 - 获取锁成功后正常分发")
    void testDispatchToClientLockAcquired() {
        // 模拟成功获取锁
        when(redisCache.setIfAbsent(
                eq("print:client:lock:1"), anyString(), eq(30L), eq(TimeUnit.SECONDS)
        )).thenReturn(true);

        // 准备链路步骤数据
        SysPrintChainItem item = new SysPrintChainItem();
        item.setScreenshotMode("DISABLED");

        SysPrintTask task = new SysPrintTask();
        task.setTaskId(1L);
        task.setClientId(1L);
        task.setTaskCode("TEST002");
        task.setChainItemId(100L);

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(chainItemMapper.selectById(100L)).thenReturn(item);

        executorService.executeStep(1L);

        // 验证状态被标记为 PRINTING
        verify(taskMapper).updateById(argThat(t ->
                "PRINTING".equals(t.getStatus())
        ));
        // 验证 MQ 消息已发送
        verify(taskProducer).sendTaskToClient(any(SysPrintTask.class), eq(1L));
        // 验证锁已在 finally 中清理
        verify(redisCache).delete("print:client:lock:1");
    }
}
