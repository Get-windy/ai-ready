package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.model.DataSource;
import cn.aiedge.datasource.service.DataSourceService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 数据源服务实现
 */
@Service
public class DataSourceServiceImpl implements DataSourceService {

    private final List<DataSource> dataSourceList = new CopyOnWriteArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @PostConstruct
    public void init() {
        DataSource ds1 = new DataSource();
        ds1.setId(idCounter.getAndIncrement());
        ds1.setName("本地MySQL");
        ds1.setDbType("MySQL");
        ds1.setHost("localhost");
        ds1.setPort(3306);
        ds1.setDatabaseName("ai_ready");
        ds1.setUsername("root");
        ds1.setPassword("******");
        ds1.setStatus("connected");
        ds1.setDescription("本地开发数据库");
        ds1.setTenantId(1L);
        ds1.setCreateTime(LocalDateTime.now().minusDays(30));
        ds1.setUpdateTime(LocalDateTime.now().minusHours(2));
        ds1.setCreateBy("admin");
        ds1.setUpdateBy("admin");
        ds1.setDeleted(false);
        dataSourceList.add(ds1);

        DataSource ds2 = new DataSource();
        ds2.setId(idCounter.getAndIncrement());
        ds2.setName("测试PostgreSQL");
        ds2.setDbType("PostgreSQL");
        ds2.setHost("192.168.1.100");
        ds2.setPort(5432);
        ds2.setDatabaseName("test_db");
        ds2.setUsername("test_user");
        ds2.setPassword("******");
        ds2.setStatus("connected");
        ds2.setDescription("测试环境PostgreSQL数据库");
        ds2.setTenantId(1L);
        ds2.setCreateTime(LocalDateTime.now().minusDays(15));
        ds2.setUpdateTime(LocalDateTime.now().minusDays(1));
        ds2.setCreateBy("admin");
        ds2.setUpdateBy("admin");
        ds2.setDeleted(false);
        dataSourceList.add(ds2);

        DataSource ds3 = new DataSource();
        ds3.setId(idCounter.getAndIncrement());
        ds3.setName("生产Oracle");
        ds3.setDbType("Oracle");
        ds3.setHost("10.0.0.50");
        ds3.setPort(1521);
        ds3.setDatabaseName("proddb");
        ds3.setUsername("prod_user");
        ds3.setPassword("******");
        ds3.setStatus("error");
        ds3.setDescription("生产环境Oracle数据库（连接异常）");
        ds3.setTenantId(1L);
        ds3.setCreateTime(LocalDateTime.now().minusDays(60));
        ds3.setUpdateTime(LocalDateTime.now().minusDays(3));
        ds3.setCreateBy("admin");
        ds3.setUpdateBy("admin");
        ds3.setDeleted(false);
        dataSourceList.add(ds3);

        DataSource ds4 = new DataSource();
        ds4.setId(idCounter.getAndIncrement());
        ds4.setName("分析SQLServer");
        ds4.setDbType("SQLServer");
        ds4.setHost("192.168.2.200");
        ds4.setPort(1433);
        ds4.setDatabaseName("analysis");
        ds4.setUsername("sa");
        ds4.setPassword("******");
        ds4.setStatus("disconnected");
        ds4.setDescription("数据分析SQLServer数据库");
        ds4.setTenantId(2L);
        ds4.setCreateTime(LocalDateTime.now().minusDays(7));
        ds4.setUpdateTime(LocalDateTime.now().minusDays(7));
        ds4.setCreateBy("analyst");
        ds4.setUpdateBy("analyst");
        ds4.setDeleted(false);
        dataSourceList.add(ds4);
    }

    @Override
    public List<DataSource> list(String keyword, Long tenantId) {
        return dataSourceList.stream()
                .filter(ds -> !ds.getDeleted())
                .filter(ds -> tenantId == null || tenantId.equals(ds.getTenantId()))
                .filter(ds -> keyword == null || keyword.isEmpty()
                        || ds.getName().toLowerCase().contains(keyword.toLowerCase())
                        || ds.getDbType().toLowerCase().contains(keyword.toLowerCase())
                        || ds.getDatabaseName().toLowerCase().contains(keyword.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public DataSource getById(Long id) {
        return dataSourceList.stream()
                .filter(ds -> id.equals(ds.getId()) && !ds.getDeleted())
                .findFirst()
                .orElse(null);
    }

    @Override
    public DataSource create(DataSource dataSource, Long tenantId) {
        dataSource.setId(idCounter.getAndIncrement());
        dataSource.setStatus("disconnected");
        dataSource.setTenantId(tenantId);
        dataSource.setCreateTime(LocalDateTime.now());
        dataSource.setUpdateTime(LocalDateTime.now());
        dataSource.setDeleted(false);
        dataSourceList.add(dataSource);
        return dataSource;
    }

    @Override
    public DataSource update(Long id, DataSource dataSource, Long tenantId) {
        DataSource existing = getById(id);
        if (existing == null) {
            return null;
        }
        if (dataSource.getName() != null) existing.setName(dataSource.getName());
        if (dataSource.getDbType() != null) existing.setDbType(dataSource.getDbType());
        if (dataSource.getHost() != null) existing.setHost(dataSource.getHost());
        if (dataSource.getPort() != null) existing.setPort(dataSource.getPort());
        if (dataSource.getDatabaseName() != null) existing.setDatabaseName(dataSource.getDatabaseName());
        if (dataSource.getUsername() != null) existing.setUsername(dataSource.getUsername());
        if (dataSource.getPassword() != null) existing.setPassword(dataSource.getPassword());
        if (dataSource.getDescription() != null) existing.setDescription(dataSource.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        if (tenantId != null) existing.setTenantId(tenantId);
        return existing;
    }

    @Override
    public boolean delete(Long id) {
        DataSource existing = getById(id);
        if (existing == null) {
            return false;
        }
        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        return true;
    }

    @Override
    public boolean testConnection(Long id) {
        DataSource ds = getById(id);
        if (ds == null) {
            return false;
        }
        // Simulate test: mark as connected for MySQL/PostgreSQL, error for others
        if ("MySQL".equalsIgnoreCase(ds.getDbType()) || "PostgreSQL".equalsIgnoreCase(ds.getDbType())) {
            ds.setStatus("connected");
            return true;
        } else {
            ds.setStatus("error");
            return false;
        }
    }
}
