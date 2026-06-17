package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.model.SecurityPolicy;
import cn.aiedge.platform.service.SecurityPolicyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全策略服务实现（内存模式）
 */
@Slf4j
@Service
public class SecurityPolicyServiceImpl implements SecurityPolicyService {

    private final Map<Long, SecurityPolicy> policyStore = new ConcurrentHashMap<>();

    @Override
    public SecurityPolicy getPolicy(Long tenantId) {
        SecurityPolicy policy = policyStore.get(tenantId);
        if (policy == null) {
            policy = createDefaultPolicy(tenantId);
            policyStore.put(tenantId, policy);
        }
        return policy;
    }

    @Override
    public SecurityPolicy savePolicy(SecurityPolicy policy, Long tenantId) {
        SecurityPolicy existing = policyStore.get(tenantId);
        if (existing == null) {
            policy.setId(System.currentTimeMillis());
            policy.setCreateTime(LocalDateTime.now());
            policy.setCreateBy("system");
        } else {
            policy.setId(existing.getId());
            policy.setCreateTime(existing.getCreateTime());
            policy.setCreateBy(existing.getCreateBy());
        }
        policy.setTenantId(tenantId);
        policy.setUpdateTime(LocalDateTime.now());
        policy.setUpdateBy("system");
        policyStore.put(tenantId, policy);
        log.info("保存安全策略: tenantId={}, lockThreshold={}, passwordMinLength={}",
                tenantId, policy.getLockThreshold(), policy.getPasswordMinLength());
        return policy;
    }

    /**
     * 创建默认安全策略（匹配前端初始值）
     */
    private SecurityPolicy createDefaultPolicy(Long tenantId) {
        SecurityPolicy policy = new SecurityPolicy();
        policy.setId(System.currentTimeMillis());
        policy.setLockThreshold(5);
        policy.setLockDuration(30);
        policy.setCaptchaEnabled(true);
        policy.setTwoFactorEnabled(false);
        policy.setPasswordMinLength(8);
        policy.setRequireUpper(true);
        policy.setRequireLower(true);
        policy.setRequireDigit(true);
        policy.setRequireSpecial(false);
        policy.setPasswordExpireDays(90);
        policy.setSessionTimeout(3600);
        policy.setSingleDevice(false);
        policy.setIpWhitelist("");
        policy.setRateLimit(true);
        policy.setAuditRetentionDays(180);
        policy.setLogSensitiveOps(true);
        policy.setLogLogin(true);
        policy.setEnabled(true);
        policy.setTenantId(tenantId);
        policy.setCreateTime(LocalDateTime.now());
        policy.setUpdateTime(LocalDateTime.now());
        policy.setCreateBy("system");
        policy.setUpdateBy("system");
        return policy;
    }
}
