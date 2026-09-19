package cn.aiedge.config.service;

import cn.aiedge.config.model.ConfigChangeLog;
import cn.aiedge.config.model.SystemConfig;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口（系统参数页 / 支付配置页共用的 `/api/config` 后端）
 *
 * <p><b>2026-09-18 改造</b>：本接口的实现原先全部作用于 JVM 内存静态 Map（12 条内置配置），
 * 与 {@code sys_config} 表完全无关（见《设置模块/系统参数开发文档.md》§5.2 / §9.2）。
 * 现在全部方法都**真实读写 {@code sys_config}**（{@code SysConfigMapper}）。</p>
 *
 * <p>查询口径：{@code tenantId} 参数保留原签名（控制器从 {@code X-Tenant-Id} 头透传）；
 * 为空时实现层回退到会话租户。读取范围为「{@code tenant_id = 0} 全局默认行 + 当前租户行」，
 * 原因与写法见 {@code SysConfigMapper} 类注释。</p>
 */
public interface SystemConfigService {

    // 获取配置
    SystemConfig getConfig(Long id);
    SystemConfig getConfigByKey(String configKey, Long tenantId);
    String getConfigValue(String configKey, Long tenantId);
    <T> T getConfigValue(String configKey, Class<T> clazz, T defaultValue, Long tenantId);

    // 获取配置列表
    List<SystemConfig> getConfigList(String configType, String configGroup, Long tenantId);

    /**
     * 按查询条件取列表（支持左标签视图 navGroup / 配置键 / 名称 / 启用 / 内置 等条件）
     *
     * @param query 查询条件载体（复用 SystemConfig 的字段；仅读取 configType/configGroup/navGroup/
     *              configKey/configName/enabled/systemConfig，其余字段忽略）
     */
    List<SystemConfig> getConfigList(SystemConfig query, Long tenantId);

    /**
     * 按查询条件**分页**取列表（LIMIT/OFFSET 真分页，pageNum/pageSize 真正生效）
     *
     * @param pageNum  页码（从 1 开始）
     * @param pageSize 每页条数（<= 0 表示不分页）
     */
    List<SystemConfig> getConfigPage(SystemConfig query, int pageNum, int pageSize, Long tenantId);

    /** 与 {@link #getConfigList(SystemConfig, Long)} 同条件的总数（真分页的 total 口径） */
    long getConfigCount(SystemConfig query, Long tenantId);

    Map<String, String> getConfigMap(String configGroup, Long tenantId);

    // 保存配置
    SystemConfig saveConfig(SystemConfig config, Long tenantId);
    void saveConfigValue(String configKey, String configValue, Long tenantId);
    void batchSaveConfigs(Map<String, String> configs, Long tenantId);

    // 删除配置
    boolean deleteConfig(Long id, Long tenantId);
    boolean deleteConfigByKey(String configKey, Long tenantId);
    boolean batchDelete(List<Long> ids, Long tenantId);

    // 配置变更日志
    List<ConfigChangeLog> getConfigChangeLogs(String configKey, Long tenantId);
    void logConfigChange(SystemConfig config, String oldValue, String changeType,
                         Long operatorId, String operatorName, String reason, Long tenantId);

    // 刷新缓存（返回实际清理的历史缓存键数量，供接口回传「真的做了什么」）
    long refreshCache(Long tenantId);
    long refreshCache(String configKey, Long tenantId);
}
