package cn.aiedge.tenant.service.impl;

import cn.aiedge.tenant.mapper.SysTenantPackageMapper;
import cn.aiedge.tenant.model.SysTenantPackage;
import cn.aiedge.tenant.service.TenantPackageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPackageServiceImpl implements TenantPackageService {

    private final SysTenantPackageMapper sysTenantPackageMapper;

    @Override
    public List<SysTenantPackage> getList() {
        LambdaQueryWrapper<SysTenantPackage> wrapper = new LambdaQueryWrapper<SysTenantPackage>()
                .eq(SysTenantPackage::getDeleted, 0)
                .orderByAsc(SysTenantPackage::getSortOrder);
        return sysTenantPackageMapper.selectList(wrapper);
    }

    @Override
    public SysTenantPackage getById(Long id) {
        return sysTenantPackageMapper.selectById(id);
    }

    @Override
    public SysTenantPackage create(SysTenantPackage pkg) {
        LocalDateTime now = LocalDateTime.now();
        pkg.setDeleted(0);
        pkg.setCreateTime(now);
        pkg.setUpdateTime(now);
        if (pkg.getStatus() == null) pkg.setStatus(1);
        sysTenantPackageMapper.insert(pkg);
        log.info("创建套餐: id={}, name={}", pkg.getId(), pkg.getPackageName());
        return pkg;
    }

    @Override
    public SysTenantPackage update(SysTenantPackage pkg) {
        SysTenantPackage existing = sysTenantPackageMapper.selectById(pkg.getId());
        if (existing == null || existing.getDeleted() == 1) return null;
        pkg.setCreateTime(existing.getCreateTime());
        pkg.setUpdateTime(LocalDateTime.now());
        pkg.setDeleted(existing.getDeleted());
        sysTenantPackageMapper.updateById(pkg);
        log.info("更新套餐: id={}, name={}", pkg.getId(), pkg.getPackageName());
        return pkg;
    }

    @Override
    public boolean delete(Long id) {
        SysTenantPackage p = sysTenantPackageMapper.selectById(id);
        if (p == null || p.getDeleted() == 1) return false;
        p.setDeleted(1);
        p.setUpdateTime(LocalDateTime.now());
        sysTenantPackageMapper.updateById(p);
        log.info("删除套餐: id={}", id);
        return true;
    }
}
