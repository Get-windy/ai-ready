package cn.aiedge.tenant.service;

import cn.aiedge.tenant.model.SysTenantPackage;

import java.util.List;

/**
 * 租户套餐服务接口
 */
public interface TenantPackageService {

    List<SysTenantPackage> getList();

    SysTenantPackage getById(Long id);

    SysTenantPackage create(SysTenantPackage pkg);

    SysTenantPackage update(SysTenantPackage pkg);

    boolean delete(Long id);
}
