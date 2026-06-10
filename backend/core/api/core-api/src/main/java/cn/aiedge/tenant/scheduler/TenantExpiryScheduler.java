package cn.aiedge.tenant.scheduler;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.base.mapper.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 租户过期自动冻结定时任务
 * <p>
 * 每天凌晨检查所有已启用的租户，对已过期的租户自动停用（status=0），
 * 同时禁用该租户下的所有活跃用户。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantExpiryScheduler {

    private final TenantMapper tenantMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 每天凌晨 2:00 执行一次租户过期检查
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void checkExpiredTenants() {
        log.info("[租户过期检查] 开始扫描过期租户...");

        // 查询所有已启用且已过期的租户
        List<SysTenant> expiredTenants = tenantMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysTenant>()
                        .eq(SysTenant::getStatus, 1)
                        .eq(SysTenant::getDeleted, 0)
                        .isNotNull(SysTenant::getExpireTime)
                        .lt(SysTenant::getExpireTime, LocalDateTime.now())
        );

        if (expiredTenants.isEmpty()) {
            log.info("[租户过期检查] 无过期租户需要处理");
            return;
        }

        int expiredCount = 0;
        int disabledUserCount = 0;

        for (SysTenant tenant : expiredTenants) {
            try {
                // 1. 停用租户
                tenant.setStatus(0);
                tenant.setUpdateTime(LocalDateTime.now());
                tenantMapper.updateById(tenant);

                // 2. 停用该租户下的所有活跃用户
                int updated = sysUserMapper.update(
                        null,
                        new LambdaUpdateWrapper<cn.aiedge.base.entity.SysUser>()
                                .eq(cn.aiedge.base.entity.SysUser::getTenantId, tenant.getId())
                                .eq(cn.aiedge.base.entity.SysUser::getStatus, 1)
                                .eq(cn.aiedge.base.entity.SysUser::getDeleted, 0)
                                .set(cn.aiedge.base.entity.SysUser::getStatus, 0)
                                .set(cn.aiedge.base.entity.SysUser::getUpdateTime, LocalDateTime.now())
                );

                expiredCount++;
                disabledUserCount += updated;

                log.info("[租户过期检查] 已冻结: tenantId={}, tenantName={}, expireTime={}, 禁用用户数={}",
                        tenant.getId(), tenant.getTenantName(), tenant.getExpireTime(), updated);
            } catch (Exception e) {
                log.error("[租户过期检查] 处理租户失败: tenantId={}, tenantName={}",
                        tenant.getId(), tenant.getTenantName(), e);
            }
        }

        log.info("[租户过期检查] 完成: 共处理过期租户={}个, 禁用用户={}个", expiredCount, disabledUserCount);
    }
}
