package cn.aiedge.datasource.service;

import cn.aiedge.datasource.model.DataSource;
import java.util.List;

/**
 * 数据源服务接口
 */
public interface DataSourceService {

    List<DataSource> list(String keyword, Long tenantId);

    DataSource getById(Long id);

    DataSource create(DataSource dataSource, Long tenantId);

    DataSource update(Long id, DataSource dataSource, Long tenantId);

    boolean delete(Long id);

    boolean testConnection(Long id);
}
