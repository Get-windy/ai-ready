package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.model.SlowQuery;
import cn.aiedge.datasource.service.SlowQueryService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 慢查询服务实现
 */
@Service
public class SlowQueryServiceImpl implements SlowQueryService {

    private final List<SlowQuery> slowQueryList = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @PostConstruct
    public void init() {
        SlowQuery sq1 = new SlowQuery();
        sq1.setId(idCounter.getAndIncrement());
        sq1.setDataSourceId(1L);
        sq1.setQueryText("SELECT * FROM sys_log WHERE create_time BETWEEN '2026-01-01' AND '2026-06-01' ORDER BY create_time DESC");
        sq1.setQueryTimeMs(12500L);
        sq1.setLockTimeMs(320L);
        sq1.setRowsExamined(1500000L);
        sq1.setRowsSent(5000L);
        sq1.setQueryTime(LocalDateTime.now().minusHours(1));
        sq1.setDatabaseName("ai_ready");
        sq1.setUserName("app_user");
        sq1.setHostInfo("192.168.1.10:3306");
        sq1.setTenantId(1L);
        sq1.setCreateTime(LocalDateTime.now().minusHours(1));
        slowQueryList.add(sq1);

        SlowQuery sq2 = new SlowQuery();
        sq2.setId(idCounter.getAndIncrement());
        sq2.setDataSourceId(1L);
        sq2.setQueryText("SELECT u.name, COUNT(o.id) FROM sys_user u LEFT JOIN sys_order o ON u.id = o.user_id GROUP BY u.name HAVING COUNT(o.id) > 10");
        sq2.setQueryTimeMs(8900L);
        sq2.setLockTimeMs(150L);
        sq2.setRowsExamined(800000L);
        sq2.setRowsSent(120L);
        sq2.setQueryTime(LocalDateTime.now().minusHours(2));
        sq2.setDatabaseName("ai_ready");
        sq2.setUserName("report_user");
        sq2.setHostInfo("192.168.1.11:3306");
        sq2.setTenantId(1L);
        sq2.setCreateTime(LocalDateTime.now().minusHours(2));
        slowQueryList.add(sq2);

        SlowQuery sq3 = new SlowQuery();
        sq3.setId(idCounter.getAndIncrement());
        sq3.setDataSourceId(2L);
        sq3.setQueryText("UPDATE sys_inventory SET quantity = quantity - 1 WHERE product_id IN (SELECT product_id FROM sys_order_items WHERE order_id = ?)");
        sq3.setQueryTimeMs(15600L);
        sq3.setLockTimeMs(4200L);
        sq3.setRowsExamined(200000L);
        sq3.setRowsSent(0L);
        sq3.setQueryTime(LocalDateTime.now().minusHours(3));
        sq3.setDatabaseName("test_db");
        sq3.setUserName("batch_job");
        sq3.setHostInfo("192.168.1.100:5432");
        sq3.setTenantId(1L);
        sq3.setCreateTime(LocalDateTime.now().minusHours(3));
        slowQueryList.add(sq3);

        SlowQuery sq4 = new SlowQuery();
        sq4.setId(idCounter.getAndIncrement());
        sq4.setDataSourceId(1L);
        sq4.setQueryText("SELECT * FROM sys_audit_log WHERE operation_type = 'DELETE' AND create_time > NOW() - INTERVAL 30 DAY");
        sq4.setQueryTimeMs(5200L);
        sq4.setLockTimeMs(80L);
        sq4.setRowsExamined(350000L);
        sq4.setRowsSent(2500L);
        sq4.setQueryTime(LocalDateTime.now().minusHours(5));
        sq4.setDatabaseName("ai_ready");
        sq4.setUserName("audit_user");
        sq4.setHostInfo("192.168.1.12:3306");
        sq4.setTenantId(1L);
        sq4.setCreateTime(LocalDateTime.now().minusHours(5));
        slowQueryList.add(sq4);

        SlowQuery sq5 = new SlowQuery();
        sq5.setId(idCounter.getAndIncrement());
        sq5.setDataSourceId(2L);
        sq5.setQueryText("SELECT p.name, SUM(oi.quantity) as total_qty FROM products p JOIN order_items oi ON p.id = oi.product_id WHERE oi.created_at > NOW() - INTERVAL '7 days' GROUP BY p.name ORDER BY total_qty DESC");
        sq5.setQueryTimeMs(7800L);
        sq5.setLockTimeMs(200L);
        sq5.setRowsExamined(500000L);
        sq5.setRowsSent(300L);
        sq5.setQueryTime(LocalDateTime.now().minusDays(1));
        sq5.setDatabaseName("test_db");
        sq5.setUserName("analyst");
        sq5.setHostInfo("192.168.1.100:5432");
        sq5.setTenantId(1L);
        sq5.setCreateTime(LocalDateTime.now().minusDays(1));
        slowQueryList.add(sq5);
    }

    @Override
    public List<SlowQuery> list(Long dataSourceId, Long tenantId) {
        return slowQueryList.stream()
                .filter(sq -> dataSourceId == null || dataSourceId.equals(sq.getDataSourceId()))
                .filter(sq -> tenantId == null || tenantId.equals(sq.getTenantId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SlowQuery> export(Long dataSourceId, Long tenantId) {
        // Export returns same data as list
        return list(dataSourceId, tenantId);
    }
}
