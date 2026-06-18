package cn.aiedge.tenant.service;

import cn.aiedge.tenant.model.SysTenantQuota;

import java.util.List;

/**
 * 租户配额服务接口
 */
public interface TenantQuotaService {

    List<SysTenantQuota> getList();

    SysTenantQuota getById(Long id);

    SysTenantQuota create(SysTenantQuota quota);

    SysTenantQuota update(SysTenantQuota quota);

    boolean delete(Long id);
}
