package cn.aiedge.erp.printing.service;

import cn.aiedge.base.security.SecurityContext;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.printing.dto.PrinterStatusDTO;
import cn.aiedge.erp.printing.entity.Printer;
import cn.aiedge.erp.printing.mapper.PrintTaskMapper;
import cn.aiedge.erp.printing.mapper.PrinterGroupMapper;
import cn.aiedge.erp.printing.mapper.PrinterMapper;
import cn.aiedge.erp.printing.service.impl.PrinterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PrinterServiceImpl 核心业务逻辑单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PrinterServiceImpl 业务逻辑测试")
class PrinterServiceImplTest {

    @Mock
    private PrinterMapper printerMapper;
    @Mock
    private PrinterGroupMapper groupMapper;
    @Mock
    private PrintTaskMapper taskMapper;
    @Mock
    private SecurityContext securityContext;

    private PrinterServiceImpl service;

    private static final Long TENANT_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new PrinterServiceImpl(printerMapper, groupMapper, taskMapper, securityContext);
        lenient().when(securityContext.getCurrentTenantId()).thenReturn(TENANT_ID);
    }

    @Nested
    @DisplayName("getPrinterStatus()")
    class GetPrinterStatusTest {

        @Test
        @DisplayName("成功获取打印机状态并传递 tenantId")
        void success() {
            Printer printer = new Printer();
            printer.setId(1L);
            printer.setPrinterName("测试打印机");
            printer.setStatus(1);
            printer.setIsOnline(true);

            when(printerMapper.selectById(1L)).thenReturn(printer);
            when(taskMapper.countActiveTasksByPrinter(1L, TENANT_ID)).thenReturn(3);

            PrinterStatusDTO dto = service.getPrinterStatus(1L);

            assertNotNull(dto);
            assertEquals("测试打印机", dto.getPrinterName());
            assertEquals("ONLINE", dto.getStatus());
            assertEquals(3, dto.getActiveTasks());
            verify(taskMapper).countActiveTasksByPrinter(1L, TENANT_ID);
        }

        @Test
        @DisplayName("打印机不存在时抛出 BusinessException")
        void notFound() {
            when(printerMapper.selectById(999L)).thenReturn(null);

            assertThrows(BusinessException.class, () -> service.getPrinterStatus(999L));
            verify(taskMapper, never()).countActiveTasksByPrinter(anyLong(), anyLong());
        }
    }
}
