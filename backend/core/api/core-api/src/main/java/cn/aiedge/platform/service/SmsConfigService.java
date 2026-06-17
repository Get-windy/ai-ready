package cn.aiedge.platform.service;

import cn.aiedge.platform.model.SmsConfig;

/**
 * 短信配置服务接口
 */
public interface SmsConfigService {

    /**
     * 获取短信配置
     */
    SmsConfig getConfig(Long tenantId);

    /**
     * 保存短信配置
     */
    SmsConfig saveConfig(SmsConfig config, Long tenantId);

    /**
     * 测试短信服务
     */
    boolean testConnection(SmsConfig config);
}
