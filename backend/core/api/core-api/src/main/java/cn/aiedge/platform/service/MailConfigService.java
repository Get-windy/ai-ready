package cn.aiedge.platform.service;

import cn.aiedge.platform.dto.ConnectionTestResult;
import cn.aiedge.platform.model.MailConfig;

/**
 * 邮件配置服务接口
 */
public interface MailConfigService {

    /**
     * 获取邮件配置
     */
    MailConfig getConfig(Long tenantId);

    /**
     * 保存邮件配置
     */
    MailConfig saveConfig(MailConfig config, Long tenantId);

    /**
     * 测试SMTP连接
     */
    ConnectionTestResult testConnection(MailConfig config);
}
