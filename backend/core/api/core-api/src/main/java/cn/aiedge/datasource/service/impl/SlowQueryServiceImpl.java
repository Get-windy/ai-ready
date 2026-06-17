package cn.aiedge.datasource.service.impl;

import cn.aiedge.datasource.mapper.SlowQueryMapper;
import cn.aiedge.datasource.model.SlowQuery;
import cn.aiedge.datasource.service.SlowQueryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 慢查询服务实现
 */
@Service
@RequiredArgsConstructor
public class SlowQueryServiceImpl implements SlowQueryService {

    private final SlowQueryMapper slowQueryMapper;

    @Override
    public List<SlowQuery> list(Long dataSourceId, Long tenantId) {
        LambdaQueryWrapper<SlowQuery> wrapper = new LambdaQueryWrapper<SlowQuery>()
                .eq(dataSourceId != null, SlowQuery::getDataSourceId, dataSourceId)
                .eq(tenantId != null, SlowQuery::getTenantId, tenantId)
                .orderByDesc(SlowQuery::getQueryTime);
        return slowQueryMapper.selectList(wrapper);
    }

    @Override
    public List<SlowQuery> export(Long dataSourceId, Long tenantId) {
        return list(dataSourceId, tenantId);
    }
}
