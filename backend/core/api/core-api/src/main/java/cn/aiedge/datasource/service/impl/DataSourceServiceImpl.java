package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.mapper.DataSourceMapper;
import cn.aiedge.datasource.model.DataSource;
import cn.aiedge.datasource.service.DataSourceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

/**
 * 数据源服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceServiceImpl implements DataSourceService {

    private final DataSourceMapper dataSourceMapper;

    @Override
    public List<DataSource> list(String keyword, Long tenantId) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<DataSource>()
                .eq(DataSource::getDeleted, 0)
                .eq(tenantId != null, DataSource::getTenantId, tenantId)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(DataSource::getName, keyword)
                        .or()
                        .like(DataSource::getDbType, keyword)
                        .or()
                        .like(DataSource::getDatabaseName, keyword))
                .orderByDesc(DataSource::getCreateTime);
        return dataSourceMapper.selectList(wrapper);
    }

    @Override
    public DataSource getById(Long id) {
        return dataSourceMapper.selectById(id);
    }

    @Override
    public DataSource create(DataSource dataSource, Long tenantId) {
        dataSource.setStatus("disconnected");
        dataSource.setTenantId(tenantId);
        dataSource.setCreateTime(LocalDateTime.now());
        dataSource.setUpdateTime(LocalDateTime.now());
        dataSource.setDeleted(0);
        dataSourceMapper.insert(dataSource);
        return dataSource;
    }

    @Override
    public DataSource update(Long id, DataSource dataSource, Long tenantId) {
        DataSource existing = dataSourceMapper.selectById(id);
        if (existing == null) return null;
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
        dataSourceMapper.updateById(existing);
        return existing;
    }

    @Override
    public boolean delete(Long id) {
        DataSource existing = dataSourceMapper.selectById(id);
        if (existing == null) return false;
        existing.setDeleted(1);
        existing.setUpdateTime(LocalDateTime.now());
        return dataSourceMapper.updateById(existing) > 0;
    }

    /**
     * 构建 JDBC URL
     */
    private String buildJdbcUrl(DataSource ds) {
        String dbType = ds.getDbType() != null ? ds.getDbType().toLowerCase() : "mysql";
        String host = ds.getHost() != null ? ds.getHost() : "localhost";
        int port = ds.getPort() != null ? ds.getPort() : 3306;
        String database = ds.getDatabaseName() != null ? ds.getDatabaseName() : "";

        switch (dbType) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&connectTimeout=5000&socketTimeout=5000", host, port, database);
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s?connectTimeout=5&socketTimeout=5", host, port, database);
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, database);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;loginTimeout=5", host, port, database);
            default:
                log.warn("不支持的数据库类型: {}, 默认使用MySQL格式", dbType);
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&connectTimeout=5000&socketTimeout=5000", host, port, database);
        }
    }

    @Override
    public boolean testConnection(Long id) {
        DataSource ds = dataSourceMapper.selectById(id);
        if (ds == null) return false;

        String url = buildJdbcUrl(ds);
        Properties connProps = new Properties();
        connProps.setProperty("user", ds.getUsername() != null ? ds.getUsername() : "");
        connProps.setProperty("password", ds.getPassword() != null ? ds.getPassword() : "");

        try (Connection conn = DriverManager.getConnection(url, connProps)) {
            boolean valid = conn.isValid(5);
            ds.setStatus(valid ? "connected" : "error");
            log.info("数据源连接测试: id={}, name={}, status={}", id, ds.getName(), valid ? "connected" : "error");
            return valid;
        } catch (SQLException e) {
            ds.setStatus("error");
            log.error("数据源连接测试失败: id={}, name={}, url={}", id, ds.getName(), url, e);
            return false;
        } finally {
            dataSourceMapper.updateById(ds);
        }
    }
}
