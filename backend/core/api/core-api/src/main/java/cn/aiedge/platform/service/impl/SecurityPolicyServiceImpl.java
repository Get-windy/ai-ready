package cn.aiedge.platform.service.impl;

import cn.aiedge.platform.mapper.SecurityPolicyMapper;
import cn.aiedge.platform.model.SecurityPolicy;
import cn.aiedge.platform.service.SecurityPolicyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityPolicyServiceImpl implements SecurityPolicyService {

    private final SecurityPolicyMapper securityPolicyMapper;

    @Override
    public SecurityPolicy getPolicy(Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        SecurityPolicy policy = securityPolicyMapper.selectOne(
                new LambdaQueryWrapper<SecurityPolicy>()
                        .eq(SecurityPolicy::getTenantId, tenantId)
        );
        if (policy == null) {
            policy = securityPolicyMapper.selectOne(
                    new LambdaQueryWrapper<SecurityPolicy>().last("LIMIT 1")
            );
        }
        return policy;
    }

    @Override
    public SecurityPolicy savePolicy(SecurityPolicy policy, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        SecurityPolicy existing = securityPolicyMapper.selectOne(
                new LambdaQueryWrapper<SecurityPolicy>()
                        .eq(SecurityPolicy::getTenantId, tenantId)
        );
        policy.setTenantId(tenantId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            policy.setCreateTime(now);
            policy.setUpdateTime(now);
            policy.setCreateBy("system");
            policy.setUpdateBy("system");
            securityPolicyMapper.insert(policy);
        } else {
            policy.setId(existing.getId());
            policy.setCreateTime(existing.getCreateTime());
            policy.setCreateBy(existing.getCreateBy());
            policy.setUpdateTime(now);
            policy.setUpdateBy("system");
            securityPolicyMapper.updateById(policy);
        }
        log.info("保存安全策略: tenantId={}, lockThreshold={}, passwordMinLength={}",
                tenantId, policy.getLockThreshold(), policy.getPasswordMinLength());
        return policy;
    }
}
