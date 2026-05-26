package cn.aiedge.integration.service;

import cn.aiedge.integration.model.*;

import java.util.List;
import java.util.Map;

/**
 * 系统集成服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface IntegrationService {

    // ==================== 集成配置管理 ====================

    /**
     * 创建集成配置
     */
    IntegrationConfig createConfig(IntegrationConfig config);

    /**
     * 更新集成配置
     */
    IntegrationConfig updateConfig(String configId, IntegrationConfig config);

    /**
     * 删除集成配置
     */
    boolean deleteConfig(String configId);

    /**
     * 获取集成配置
     */
    IntegrationConfig getConfig(String configId);

    /**
     * 根据系统编码获取配置
     */
    IntegrationConfig getConfigBySystemCode(String systemCode);

    /**
     * 获取所有配置
     */
    List<IntegrationConfig> listConfigs();

    /**
     * 启用/禁用配置
     */
    boolean toggleConfigStatus(String configId, boolean enabled);

    // ==================== 数据同步 ====================

    /**
     * 同步用户数据
     */
    SyncRecord syncUser(String configId, String operation, Map<String, Object> userData);

    /**
     * 批量同步用户
     */
    List<SyncRecord> syncUsers(String configId, String operation, List<Map<String, Object>> users);

    /**
     * 同步订单数据
     */
    SyncRecord syncOrder(String configId, String operation, Map<String, Object> orderData);

    /**
     * 批量同步订单
     */
    List<SyncRecord> syncOrders(String configId, String operation, List<Map<String, Object>> orders);

    /**
     * 同步产品数据
     */
    SyncRecord syncProduct(String configId, String operation, Map<String, Object> productData);

    /**
     * 执行全量同步
     */
    Map<String, Object> fullSync(String configId, String syncType);

    /**
     * 执行增量同步
     */
    Map<String, Object> incrementalSync(String configId, String syncType, String lastSyncTime);

    // ==================== 同步记录查询 ====================

    /**
     * 获取同步记录
     */
    SyncRecord getSyncRecord(String recordId);

    /**
     * 查询同步记录列表
     */
    List<SyncRecord> listSyncRecords(String configId, String syncType, String status, int page, int pageSize);

    /**
     * 获取同步统计
     */
    Map<String, Object> getSyncStatistics(String configId);

    /**
     * 重试失败的同步
     */
    boolean retrySync(String recordId);

    // ==================== API调用 ====================

    /**
     * 调用外部API
     */
    ApiResponse<?> callExternalApi(String configId, ApiRequest request);

    /**
     * 发送WebHook通知
     */
    boolean sendWebhook(String configId, String eventType, Object data);

    // ==================== 鉴权和限流 ====================

    /**
     * 验证API密钥
     */
    boolean validateApiKey(String systemCode, String apiKey);

    /**
     * 验证签名
     */
    boolean validateSign(String systemCode, ApiRequest request);

    /**
     * 检查限流
     */
    boolean checkRateLimit(String systemCode);

    /**
     * 检查IP白名单
     */
    boolean checkIpWhitelist(String systemCode, String ip);

    /**
     * 生成签名
     */
    String generateSign(String systemCode, Map<String, Object> params);

    /**
     * 刷新访问令牌
     */
    boolean refreshAccessToken(String configId);

    // ==================== 健康检查 ====================

    /**
     * 检查集成健康状态
     */
    Map<String, Object> checkHealth(String configId);

    /**
     * 测试连接
     */
    boolean testConnection(String configId);
}
