package cn.aiedge.tenant.service;

import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 租户服务
 * 提供租户有效期校验、等级查询等功能
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantMapper tenantMapper;

    /**
     * 检查租户是否有效（未过期且状态正常）
     *
     * @param tenantId 租户ID
     * @return true=有效 false=无效
     */
    public boolean isTenantValid(Long tenantId) {
        if (tenantId == null) {
            return false;
        }

        try {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null || tenant.getDeleted() == 1) {
                log.warn("租户不存在或已删除: tenantId={}", tenantId);
                return false;
            }

            // 检查状态
            if (tenant.getStatus() != null && tenant.getStatus() != 0) {
                log.warn("租户已停用: tenantId={}, status={}", tenantId, tenant.getStatus());
                return false;
            }

            // 检查过期时间
            if (tenant.getExpireTime() != null && LocalDateTime.now().isAfter(tenant.getExpireTime())) {
                log.warn("租户已过期: tenantId={}, expireTime={}", tenantId, tenant.getExpireTime());
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("检查租户有效性失败: tenantId={}", tenantId, e);
            return false;
        }
    }

    /**
     * 获取租户等级
     *
     * @param tenantId 租户ID
     * @return 等级编码，默认 basic
     */
    public String getTenantLevel(Long tenantId) {
        if (tenantId == null) {
            return "basic";
        }

        try {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null) {
                return "basic";
            }
            return tenant.getLevel() != null ? tenant.getLevel() : "basic";
        } catch (Exception e) {
            log.error("获取租户等级失败: tenantId={}", tenantId, e);
            return "basic";
        }
    }

    /**
     * 获取租户到期时间
     *
     * @param tenantId 租户ID
     * @return 到期时间，可能为 null
     */
    public LocalDateTime getTenantExpireTime(Long tenantId) {
        if (tenantId == null) {
            return null;
        }

        try {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null) {
                return null;
            }
            return tenant.getExpireTime();
        } catch (Exception e) {
            log.error("获取租户到期时间失败: tenantId={}", tenantId, e);
            return null;
        }
    }
}
