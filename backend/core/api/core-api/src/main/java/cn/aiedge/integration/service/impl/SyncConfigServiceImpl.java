package cn.aiedge.integration.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.integration.mapper.SyncDataSourceConfigMapper;
import cn.aiedge.integration.model.SyncDataSourceConfig;
import cn.aiedge.integration.service.SyncConfigService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 同步数据源配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SyncConfigServiceImpl implements SyncConfigService {

    private final SyncDataSourceConfigMapper configMapper;

    @Value("${sync-engine.api-url:http://127.0.0.1:9800}")
    private String syncEngineApiUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 支持的导入系统列表 ====================

    private static final List<Map<String, String>> SUPPORTED_SOURCES = List.of(
            Map.of("systemCode", "ql361", "systemName", "来肯云商", "description", "来肯企汇 ERP 系统"),
            Map.of("systemCode", "yonyou", "systemName", "用友 U8+", "description", "用友 U8+ ERP 系统"),
            Map.of("systemCode", "kingdee", "systemName", "金蝶 K/3", "description", "金蝶 K/3 WISE ERP 系统")
    );

    // ==================== 获取当前租户 ====================

    private Long getCurrentTenantId() {
        // 从 SaToken 中获取当前租户ID
        Object tenantId = StpUtil.getExtra("tenantId");
        if (tenantId instanceof Number) {
            return ((Number) tenantId).longValue();
        }
        // 默认租户（单租户模式）
        return 1L;
    }

    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return 0L;
        }
    }

    // ==================== CRUD ====================

    @Override
    public SyncDataSourceConfig createConfig(SyncDataSourceConfig config) {
        Long tenantId = getCurrentTenantId();

        // 检查是否已存在同类型的配置
        SyncDataSourceConfig existing = configMapper.selectByTenantAndSource(tenantId, config.getSourceType());
        if (existing != null) {
            throw new BusinessException("该导入系统类型已配置，请编辑现有配置");
        }

        // 设置默认值
        config.setTenantId(tenantId);
        if (config.getSyncMode() == null) config.setSyncMode("incremental");
        if (config.getSyncCron() == null) config.setSyncCron("*/30 * * * *");
        if (config.getHeartbeatInterval() == null) config.setHeartbeatInterval(300);
        if (config.getBillTypes() == null) config.setBillTypes("[\"601\",\"604\",\"504\",\"801\"]");
        if (config.getBaseUrl() == null) config.setBaseUrl("https://www.ql361.com");
        if (config.getStatus() == null) config.setStatus(1);
        if (config.getDisplayName() == null) {
            config.setDisplayName(getSourceDisplayName(config.getSourceType()));
        }

        config.setCreateBy(getCurrentUserId());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());

        configMapper.insert(config);
        log.info("创建同步配置: tenantId={}, sourceType={}, id={}", tenantId, config.getSourceType(), config.getId());
        return config;
    }

    @Override
    public SyncDataSourceConfig updateConfig(Long id, SyncDataSourceConfig config) {
        SyncDataSourceConfig existing = configMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("配置不存在");
        }

        // 仅更新允许修改的字段
        if (config.getSourceUsername() != null) existing.setSourceUsername(config.getSourceUsername());
        if (config.getSourcePassword() != null) existing.setSourcePassword(config.getSourcePassword());
        if (config.getBaseUrl() != null) existing.setBaseUrl(config.getBaseUrl());
        if (config.getDisplayName() != null) existing.setDisplayName(config.getDisplayName());
        if (config.getSyncMode() != null) existing.setSyncMode(config.getSyncMode());
        if (config.getSyncCron() != null) existing.setSyncCron(config.getSyncCron());
        if (config.getHeartbeatInterval() != null) existing.setHeartbeatInterval(config.getHeartbeatInterval());
        if (config.getBillTypes() != null) existing.setBillTypes(config.getBillTypes());
        if (config.getRemark() != null) existing.setRemark(config.getRemark());

        existing.setUpdateBy(getCurrentUserId());
        existing.setUpdateTime(LocalDateTime.now());

        configMapper.updateById(existing);
        log.info("更新同步配置: id={}", id);
        return existing;
    }

    @Override
    public boolean deleteConfig(Long id) {
        SyncDataSourceConfig existing = configMapper.selectById(id);
        if (existing == null) {
            return false;
        }
        configMapper.deleteById(id);
        log.info("删除同步配置: id={}", id);
        return true;
    }

    @Override
    public SyncDataSourceConfig getConfig(Long id) {
        return configMapper.selectById(id);
    }

    @Override
    public List<SyncDataSourceConfig> listConfigs() {
        Long tenantId = getCurrentTenantId();
        LambdaQueryWrapper<SyncDataSourceConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SyncDataSourceConfig::getTenantId, tenantId);
        wrapper.orderByDesc(SyncDataSourceConfig::getCreateTime);
        return configMapper.selectList(wrapper);
    }

    @Override
    public boolean toggleStatus(Long id, boolean enabled) {
        SyncDataSourceConfig config = configMapper.selectById(id);
        if (config == null) {
            return false;
        }
        config.setStatus(enabled ? 1 : 0);
        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);
        log.info("{} 同步配置: id={}", enabled ? "启用" : "禁用", id);
        return true;
    }

    // ==================== 操作 ====================

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> testConnection(Long configId) {
        SyncDataSourceConfig config = configMapper.selectById(configId);
        if (config == null) {
            return Map.of("connected", false, "message", "配置不存在");
        }

        long start = System.currentTimeMillis();
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("source_type", config.getSourceType());
            body.put("username", config.getSourceUsername());
            body.put("password", config.getSourcePassword());
            body.put("base_url", config.getBaseUrl());

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(syncEngineApiUrl + "/api/sync/test-connection"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                log.info("测试连接: configId={}, result={}", configId, result.get("connected"));
                return result;
            } else {
                log.warn("测试连接 HTTP {}: configId={}", response.statusCode(), configId);
                return Map.of(
                        "connected", false,
                        "message", "同步引擎返回异常: " + response.statusCode(),
                        "latency", System.currentTimeMillis() - start
                );
            }
        } catch (java.net.ConnectException e) {
            log.warn("测试连接失败（同步引擎未启动）: {}", e.getMessage());
            return Map.of(
                    "connected", false,
                    "message", "同步引擎未启动，请确认 sync-engine 正在运行",
                    "latency", System.currentTimeMillis() - start
            );
        } catch (Exception e) {
            log.warn("测试连接失败: configId={}, error={}", configId, e.getMessage());
            return Map.of(
                    "connected", false,
                    "message", "连接失败: " + e.getMessage(),
                    "latency", System.currentTimeMillis() - start
            );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> triggerSync(Long configId, String syncType) {
        SyncDataSourceConfig config = configMapper.selectById(configId);
        if (config == null) {
            return Map.of("success", false, "message", "配置不存在");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("config_id", configId);
            body.put("sync_type", syncType);

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(syncEngineApiUrl + "/api/sync/trigger"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
                log.info("触发同步: configId={}, type={}, result={}", configId, syncType, result.get("success"));
                return result;
            } else {
                log.warn("触发同步 HTTP {}: configId={}", response.statusCode(), configId);
                return Map.of(
                        "success", false,
                        "message", "同步引擎返回异常: " + response.statusCode()
                );
            }
        } catch (java.net.ConnectException e) {
            log.warn("触发同步失败（同步引擎未启动）: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "message", "同步引擎未启动，请确认 sync-engine 正在运行"
            );
        } catch (Exception e) {
            log.warn("触发同步失败: configId={}, error={}", configId, e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @Override
    public List<Map<String, String>> getSupportedSources() {
        return SUPPORTED_SOURCES;
    }

    // ==================== 同步引擎 ====================

    @Override
    public List<SyncDataSourceConfig> getAllEnabledConfigs() {
        return configMapper.selectAllEnabled();
    }

    // ==================== 辅助方法 ====================

    private String getSourceDisplayName(String sourceType) {
        for (Map<String, String> source : SUPPORTED_SOURCES) {
            if (source.get("systemCode").equals(sourceType)) {
                return source.get("systemName");
            }
        }
        return sourceType;
    }
}
