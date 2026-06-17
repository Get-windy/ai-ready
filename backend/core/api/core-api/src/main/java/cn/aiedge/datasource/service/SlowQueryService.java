package cn.aiedge.datasource.service;

import cn.aiedge.datasource.model.SlowQuery;
import java.util.List;

/**
 * 慢查询服务接口
 */
public interface SlowQueryService {

    List<SlowQuery> list(Long dataSourceId, Long tenantId);

    List<SlowQuery> export(Long dataSourceId, Long tenantId);
}
