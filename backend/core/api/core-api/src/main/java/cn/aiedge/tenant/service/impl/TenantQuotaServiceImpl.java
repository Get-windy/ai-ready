package cn.aiedge.tenant.service.impl;

import cn.aiedge.tenant.mapper.SysTenantQuotaMapper;
import cn.aiedge.tenant.model.SysTenantQuota;
import cn.aiedge.tenant.service.TenantQuotaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantQuotaServiceImpl implements TenantQuotaService {

    private final SysTenantQuotaMapper sysTenantQuotaMapper;

    @Override
    public List<SysTenantQuota> getList() {
        LambdaQueryWrapper<SysTenantQuota> wrapper = new LambdaQueryWrapper<SysTenantQuota>()
                .orderByDesc(SysTenantQuota::getCreateTime);
        return sysTenantQuotaMapper.selectList(wrapper);
    }

    @Override
    public SysTenantQuota getById(Long id) {
        return sysTenantQuotaMapper.selectById(id);
    }

    @Override
    public SysTenantQuota create(SysTenantQuota quota) {
        LocalDateTime now = LocalDateTime.now();
        quota.setCreateTime(now);
        quota.setUpdateTime(now);
        sysTenantQuotaMapper.insert(quota);
        log.info("创建配额: id={}, tenantId={}", quota.getId(), quota.getTenantId());
        return quota;
    }

    @Override
    public SysTenantQuota update(SysTenantQuota quota) {
        SysTenantQuota existing = sysTenantQuotaMapper.selectById(quota.getId());
        if (existing == null) return null;
        quota.setCreateTime(existing.getCreateTime());
        quota.setUpdateTime(LocalDateTime.now());
        sysTenantQuotaMapper.updateById(quota);
        log.info("更新配额: id={}, tenantId={}", quota.getId(), quota.getTenantId());
        return quota;
    }

    @Override
    public boolean delete(Long id) {
        return sysTenantQuotaMapper.deleteById(id) > 0;
    }
}
