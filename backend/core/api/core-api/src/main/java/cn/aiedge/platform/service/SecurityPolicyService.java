package cn.aiedge.platform.service;

import cn.aiedge.platform.model.SecurityPolicy;

/**
 * 安全策略服务接口
 */
public interface SecurityPolicyService {

    /**
     * 获取安全策略
     */
    SecurityPolicy getPolicy(Long tenantId);

    /**
     * 保存安全策略
     */
    SecurityPolicy savePolicy(SecurityPolicy policy, Long tenantId);
}
