package cn.aiedge.erp.printing.service;

import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.printing.entity.PrintTask;
import cn.aiedge.erp.printing.entity.PrintTemplate;
import cn.aiedge.erp.printing.entity.Printer;
import cn.aiedge.erp.printing.enums.TaskStatus;
import cn.aiedge.erp.printing.mapper.PrintLogMapper;
import cn.aiedge.erp.printing.mapper.PrintTaskMapper;
import cn.aiedge.erp.printing.mapper.PrintTemplateMapper;
import cn.aiedge.erp.printing.mapper.PrinterMapper;
import cn.aiedge.erp.printing.service.impl.PrintTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PrintTaskServiceImpl 核心业务逻辑单元测试
 *
 * <p>使用 Mockito 隔离数据库和消息队列层，验证业务规则和异常处理。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrintTaskServiceImpl 业务逻辑测试")
class PrintTaskServiceImplTest {

    @Mock
    private PrintTaskMapper taskMapper;
    @Mock
    private PrintTemplateMapper templateMapper;
    @Mock
    private PrinterMapper printerMapper;
    @Mock
    private PrintLogMapper logMapper;
    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private SecurityContext securityContext;

    private PrintTaskServiceImpl service;

    /** 测试用租户 ID */
    private static final Long TENANT_ID = 100L;
    /** 非法租户 ID */
    private static final Long OTHER_TENANT_ID = 999L;

    @BeforeEach
    void setUp() {
        service = new PrintTaskServiceImpl(
                taskMapper, templateMapper, printerMapper,
                logMapper, rabbitTemplate, securityContext
        );
        lenient().when(securityContext.getCurrentTenantId()).thenReturn(TENANT_ID);
    }

    // ═══════════════════════════════════════════════════════════════
    // 打印任务创建
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createTask()")
    class CreateTaskTest {

        @Test
        @DisplayName("成功创建任务")
        void success() {
            PrintTemplate template = new PrintTemplate();
            template.setId(1L);
            Printer printer = new Printer();
            printer.setId(1L);
            printer.setPrinterName("测试打印机");
            printer.setIsOnline(true);

            when(templateMapper.selectById(1L)).thenReturn(template);
            when(printerMapper.selectById(1L)).thenReturn(printer);

            cn.aiedge.erp.printing.dto.PrintTaskCreateRequest request =
                    new cn.aiedge.erp.printing.dto.PrintTaskCreateRequest();
            request.setTemplateId(1L);
            request.setPrinterId(1L);

            PrintTask result = service.createTask(request, "127.0.0.1");

            assertNotNull(result);
            assertNotNull(result.getTaskCode());
            assertEquals(TaskStatus.PENDING.getCode(), result.getStatus());
            verify(taskMapper).insert(argThat((PrintTask t) ->
                    t.getTemplateId().equals(1L) && t.getPrinterId().equals(1L)
            ));
            verify(rabbitTemplate).convertAndSend(eq("print.queue"), anyLong());
        }

        @Test
        @DisplayName("模板不存在时抛出 BusinessException")
        void templateNotFound() {
            when(templateMapper.selectById(999L)).thenReturn(null);

            cn.aiedge.erp.printing.dto.PrintTaskCreateRequest request =
                    new cn.aiedge.erp.printing.dto.PrintTaskCreateRequest();
            request.setTemplateId(999L);
            request.setPrinterId(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createTask(request, "127.0.0.1"));
            assertTrue(ex.getMessage().contains("模板不存在"));
        }

        @Test
        @DisplayName("打印机离线时抛出 BusinessException")
        void printerOffline() {
            PrintTemplate template = new PrintTemplate();
            template.setId(1L);
            Printer printer = new Printer();
            printer.setId(1L);
            printer.setIsOnline(false);
            printer.setPrinterName("离线打印机");

            when(templateMapper.selectById(1L)).thenReturn(template);
            when(printerMapper.selectById(1L)).thenReturn(printer);

            cn.aiedge.erp.printing.dto.PrintTaskCreateRequest request =
                    new cn.aiedge.erp.printing.dto.PrintTaskCreateRequest();
            request.setTemplateId(1L);
            request.setPrinterId(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.createTask(request, "127.0.0.1"));
            assertTrue(ex.getMessage().contains("打印机离线"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 取消任务
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cancelTask()")
    class CancelTaskTest {

        private PrintTask buildTask(String status) {
            PrintTask task = new PrintTask();
            task.setId(1L);
            task.setTenantId(TENANT_ID);
            task.setStatus(status);
            return task;
        }

        @Test
        @DisplayName("成功取消待处理任务")
        void success() {
            PrintTask task = buildTask(TaskStatus.PENDING.getCode());
            when(taskMapper.selectById(1L)).thenReturn(task);

            service.cancelTask(1L);

            verify(taskMapper).updateById(argThat((PrintTask t) ->
                    t.getStatus().equals(TaskStatus.CANCELLED.getCode())
            ));
        }

        @Test
        @DisplayName("任务不存在时抛出 BusinessException")
        void notFound() {
            when(taskMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class, () -> service.cancelTask(999L));
            verify(taskMapper, never()).updateById(any(PrintTask.class));
        }

        @Test
        @DisplayName("跨租户任务抛出 BusinessException")
        void crossTenant() {
            PrintTask task = buildTask(TaskStatus.PENDING.getCode());
            task.setTenantId(OTHER_TENANT_ID);
            when(taskMapper.selectById(1L)).thenReturn(task);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelTask(1L));
            assertTrue(ex.getMessage().contains("无权操作"));
            verify(taskMapper, never()).updateById(any(PrintTask.class));
        }

        @Test
        @DisplayName("已完成任务不可取消")
        void alreadyCompleted() {
            PrintTask task = buildTask(TaskStatus.COMPLETED.getCode());
            when(taskMapper.selectById(1L)).thenReturn(task);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.cancelTask(1L));
            assertTrue(ex.getMessage().contains("不允许取消"));
            verify(taskMapper, never()).updateById(any(PrintTask.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 重试任务
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("retryTask()")
    class RetryTaskTest {

        private PrintTask buildFailedTask(int retryCount, int maxRetry) {
            PrintTask task = new PrintTask();
            task.setId(1L);
            task.setTenantId(TENANT_ID);
            task.setStatus(TaskStatus.FAILED.getCode());
            task.setRetryCount(retryCount);
            task.setMaxRetry(maxRetry);
            return task;
        }

        @Test
        @DisplayName("成功重试失败任务")
        void success() {
            PrintTask task = buildFailedTask(0, 3);
            when(taskMapper.selectById(1L)).thenReturn(task);

            service.retryTask(1L);

            verify(taskMapper).updateById(argThat((PrintTask t) ->
                    t.getStatus().equals(TaskStatus.RETRYING.getCode())
                            && t.getRetryCount() == 1
                            && t.getErrorMessage() == null
            ));
            verify(rabbitTemplate).convertAndSend(eq("print.queue"), anyLong());
        }

        @Test
        @DisplayName("非失败状态不可重试")
        void notFailed() {
            PrintTask task = buildFailedTask(0, 3);
            task.setStatus(TaskStatus.COMPLETED.getCode());
            when(taskMapper.selectById(1L)).thenReturn(task);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.retryTask(1L));
            assertTrue(ex.getMessage().contains("只有失败"));
            verify(taskMapper, never()).updateById(any(PrintTask.class));
        }

        @Test
        @DisplayName("超过最大重试次数不可重试")
        void maxRetryExceeded() {
            PrintTask task = buildFailedTask(3, 3);
            when(taskMapper.selectById(1L)).thenReturn(task);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> service.retryTask(1L));
            assertTrue(ex.getMessage().contains("最大重试次数"));
            verify(taskMapper, never()).updateById(any(PrintTask.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 队列查询
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("队列查询 — 多租户")
    class QueueQueryTest {

        @Test
        @DisplayName("getPrintQueue() 传递 tenantId 给 Mapper")
        void getPrintQueuePassesTenantId() {
            when(taskMapper.selectPendingTasks(100, TENANT_ID)).thenReturn(List.of());

            service.getPrintQueue();

            verify(taskMapper).selectPendingTasks(100, TENANT_ID);
        }

        @Test
        @DisplayName("getQueueLength() 传递 tenantId 给 Mapper")
        void getQueueLengthPassesTenantId() {
            when(taskMapper.countByStatus(TaskStatus.PENDING.getCode(), TENANT_ID)).thenReturn(2);
            when(taskMapper.countByStatus(TaskStatus.QUEUED.getCode(), TENANT_ID)).thenReturn(3);

            int length = service.getQueueLength();

            assertEquals(5, length);
            verify(taskMapper).countByStatus(TaskStatus.PENDING.getCode(), TENANT_ID);
            verify(taskMapper).countByStatus(TaskStatus.QUEUED.getCode(), TENANT_ID);
        }
    }
}
