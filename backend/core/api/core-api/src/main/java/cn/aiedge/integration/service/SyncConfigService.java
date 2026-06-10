package cn.aiedge.integration.service;

import cn.aiedge.integration.model.SyncDataSourceConfig;

import java.util.List;
import java.util.Map;

/**
 * 同步数据源配置服务接口
 */
public interface SyncConfigService {

    // ==================== CRUD ====================

    /**
     * 创建同步数据源配置
     */
    SyncDataSourceConfig createConfig(SyncDataSourceConfig config);

    /**
     * 更新配置
     */
    SyncDataSourceConfig updateConfig(Long id, SyncDataSourceConfig config);

    /**
     * 删除配置（逻辑删除）
     */
    boolean deleteConfig(Long id);

    /**
     * 根据ID获取配置
     */
    SyncDataSourceConfig getConfig(Long id);

    /**
     * 获取当前租户的所有配置
     */
    List<SyncDataSourceConfig> listConfigs();

    /**
     * 启用/禁用配置
     */
    boolean toggleStatus(Long id, boolean enabled);

    // ==================== 操作 ====================

    /**
     * 测试与外部系统的连接
     *
     * @param configId 配置ID
     * @return 连接结果 {connected: bool, message: str, latency: ms}
     */
    Map<String, Object> testConnection(Long configId);

    /**
     * 手动触发同步
     *
     * @param configId 配置ID
     * @param syncType full 或 incremental
     * @return 同步结果
     */
    Map<String, Object> triggerSync(Long configId, String syncType);

    /**
     * 获取支持的导入系统列表（供下拉选择）
     *
     * @return [{systemCode: "ql361", systemName: "来肯云商"}, ...]
     */
    List<Map<String, String>> getSupportedSources();

    // ==================== 同步引擎 ====================

    /**
     * 获取所有启用的配置（供同步引擎轮询）
     */
    List<SyncDataSourceConfig> getAllEnabledConfigs();
}
