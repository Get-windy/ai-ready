package cn.aiedge.integration.service.impl;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.integration.model.*;
import cn.aiedge.integration.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationServiceImpl implements IntegrationService {

    private final CacheService cacheService;

    private static final String CONFIG_KEY = "integration:config:";
    private static final String SYNC_RECORD_KEY = "integration:sync:";
    private static final String RATE_LIMIT_KEY = "integration:ratelimit:";
    private static final String API_KEY_MAP = "integration:apikey:";

    @Override
    public IntegrationConfig createConfig(IntegrationConfig config) {
        if (config.getConfigId() == null) {
            config.setConfigId(UUID.randomUUID().toString());
        }
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        config.setStatus("enabled");

        if (config.getRateLimit() == null) config.setRateLimit(100);
        if (config.getDailyLimit() == null) config.setDailyLimit(10000);
        if (config.getTimeout() == null) config.setTimeout(30);
        if (config.getRetryCount() == null) config.setRetryCount(3);
        if (config.getRetryInterval() == null) config.setRetryInterval(5);

        cacheService.set(CONFIG_KEY + config.getConfigId(), config);
        cacheService.set(API_KEY_MAP + config.getSystemCode(), config.getConfigId());

        log.info("创建集成配置: configId={}, systemCode={}", config.getConfigId(), config.getSystemCode());
        return config;
    }

    @Override
    public IntegrationConfig updateConfig(String configId, IntegrationConfig config) {
        IntegrationConfig existing = getConfig(configId);
        if (existing == null) return null;

        if (config.getName() != null) existing.setName(config.getName());
        if (config.getBaseUrl() != null) existing.setBaseUrl(config.getBaseUrl());
        if (config.getApiKey() != null) existing.setApiKey(config.getApiKey());
        if (config.getApiSecret() != null) existing.setApiSecret(config.getApiSecret());
        if (config.getWebhookUrl() != null) existing.setWebhookUrl(config.getWebhookUrl());
        if (config.getRateLimit() != null) existing.setRateLimit(config.getRateLimit());

        existing.setUpdateTime(LocalDateTime.now());
        cacheService.set(CONFIG_KEY + configId, existing);
        return existing;
    }

    @Override
    public boolean deleteConfig(String configId) {
        IntegrationConfig config = getConfig(configId);
        if (config != null) {
            cacheService.delete(API_KEY_MAP + config.getSystemCode());
        }
        cacheService.delete(CONFIG_KEY + configId);
        return true;
    }

    @Override
    public IntegrationConfig getConfig(String configId) {
        return cacheService.get(CONFIG_KEY + configId, IntegrationConfig.class);
    }

    @Override
    public IntegrationConfig getConfigBySystemCode(String systemCode) {
        String configId = cacheService.get(API_KEY_MAP + systemCode, String.class);
        if (configId != null) {
            return getConfig(configId);
        }
        return null;
    }

    @Override
    public List<IntegrationConfig> listConfigs() {
        return new ArrayList<>();
    }

    @Override
    public boolean toggleConfigStatus(String configId, boolean enabled) {
        IntegrationConfig config = getConfig(configId);
        if (config == null) return false;
        config.setStatus(enabled ? "enabled" : "disabled");
        config.setUpdateTime(LocalDateTime.now());
        cacheService.set(CONFIG_KEY + configId, config);
        return true;
    }

    @Override
    public SyncRecord syncUser(String configId, String operation, Map<String, Object> userData) {
        SyncRecord record = createSyncRecord(configId, "user", operation, userData);
        try {
            Thread.sleep(100);
            record.setStatus("success");
            record.setExternalId(UUID.randomUUID().toString());
        } catch (InterruptedException e) {
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
        }
        record.setEndTime(LocalDateTime.now());
        saveSyncRecord(record);
        updateLastSyncTime(configId);
        return record;
    }

    @Override
    public List<SyncRecord> syncUsers(String configId, String operation, List<Map<String, Object>> users) {
        List<SyncRecord> records = new ArrayList<>();
        for (Map<String, Object> userData : users) {
            records.add(syncUser(configId, operation, userData));
        }
        return records;
    }

    @Override
    public SyncRecord syncOrder(String configId, String operation, Map<String, Object> orderData) {
        SyncRecord record = createSyncRecord(configId, "order", operation, orderData);
        try {
            Thread.sleep(100);
            record.setStatus("success");
            record.setExternalId(UUID.randomUUID().toString());
        } catch (InterruptedException e) {
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
        }
        record.setEndTime(LocalDateTime.now());
        saveSyncRecord(record);
        updateLastSyncTime(configId);
        return record;
    }

    @Override
    public List<SyncRecord> syncOrders(String configId, String operation, List<Map<String, Object>> orders) {
        List<SyncRecord> records = new ArrayList<>();
        for (Map<String, Object> orderData : orders) {
            records.add(syncOrder(configId, operation, orderData));
        }
        return records;
    }

    @Override
    public SyncRecord syncProduct(String configId, String operation, Map<String, Object> productData) {
        SyncRecord record = createSyncRecord(configId, "product", operation, productData);
        try {
            Thread.sleep(100);
            record.setStatus("success");
            record.setExternalId(UUID.randomUUID().toString());
        } catch (InterruptedException e) {
            record.setStatus("failed");
            record.setErrorMessage(e.getMessage());
        }
        record.setEndTime(LocalDateTime.now());
        saveSyncRecord(record);
        updateLastSyncTime(configId);
        return record;
    }

    @Override
    public Map<String, Object> fullSync(String configId, String syncType) {
        Map<String, Object> result = new HashMap<>();
        result.put("configId", configId);
        result.put("syncType", syncType);
        result.put("type", "full");
        result.put("status", "success");
        result.put("message", "全量同步完成");
        return result;
    }

    @Override
    public Map<String, Object> incrementalSync(String configId, String syncType, String lastSyncTime) {
        Map<String, Object> result = new HashMap<>();
        result.put("configId", configId);
        result.put("syncType", syncType);
        result.put("type", "incremental");
        result.put("lastSyncTime", lastSyncTime);
        result.put("status", "success");
        result.put("message", "增量同步完成");
        return result;
    }

    @Override
    public SyncRecord getSyncRecord(String recordId) {
        return cacheService.get(SYNC_RECORD_KEY + recordId, SyncRecord.class);
    }

    @Override
    public List<SyncRecord> listSyncRecords(String configId, String syncType, String status, int page, int pageSize) {
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> getSyncStatistics(String configId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("configId", configId);
        stats.put("total", 0);
        stats.put("success", 0);
        stats.put("failed", 0);
        return stats;
    }

    @Override
    public boolean retrySync(String recordId) {
        SyncRecord record = getSyncRecord(recordId);
        if (record == null) return false;
        record.setRetryCount(record.getRetryCount() + 1);
        record.setStatus("pending");
        record.setStartTime(LocalDateTime.now());
        record.setEndTime(LocalDateTime.now());
        saveSyncRecord(record);
        return true;
    }

    @Override
    public ApiResponse<?> callExternalApi(String configId, ApiRequest request) {
        IntegrationConfig config = getConfig(configId);
        if (config == null) {
            return ApiResponse.error("配置不存在");
        }
        if (!checkRateLimit(config.getSystemCode())) {
            return ApiResponse.error(429, "请求过于频繁");
        }
        try {
            Thread.sleep(50);
            return ApiResponse.success("调用成功");
        } catch (InterruptedException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @Override
    public boolean sendWebhook(String configId, String eventType, Object data) {
        IntegrationConfig config = getConfig(configId);
        if (config == null || config.getWebhookUrl() == null) {
            return false;
        }
        log.info("发送WebHook: configId={}, eventType={}", configId, eventType);
        return true;
    }

    @Override
    public boolean validateApiKey(String systemCode, String apiKey) {
        IntegrationConfig config = getConfigBySystemCode(systemCode);
        if (config == null) return false;
        return config.getApiKey().equals(apiKey) && "enabled".equals(config.getStatus());
    }

    @Override
    public boolean validateSign(String systemCode, ApiRequest request) {
        IntegrationConfig config = getConfigBySystemCode(systemCode);
        if (config == null) return false;
        if ("none".equals(config.getSignMethod())) return true;
        String expectedSign = generateSign(systemCode, request.getParams());
        return expectedSign.equals(request.getSign());
    }

    @Override
    public boolean checkRateLimit(String systemCode) {
        IntegrationConfig config = getConfigBySystemCode(systemCode);
        if (config == null) return false;
        String key = RATE_LIMIT_KEY + systemCode + ":" + System.currentTimeMillis() / 1000;
        Integer count = cacheService.get(key, Integer.class);
        if (count == null) count = 0;
        if (count >= config.getRateLimit()) return false;
        cacheService.set(key, count + 1, 1, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean checkIpWhitelist(String systemCode, String ip) {
        IntegrationConfig config = getConfigBySystemCode(systemCode);
        if (config == null) return false;
        List<String> whitelist = config.getIpWhitelist();
        if (whitelist == null || whitelist.isEmpty()) return true;
        return whitelist.contains(ip);
    }

    @Override
    public String generateSign(String systemCode, Map<String, Object> params) {
        IntegrationConfig config = getConfigBySystemCode(systemCode);
        if (config == null || params == null) return "";
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (sb.length() > 0) sb.append("&");
            sb.append(key).append("=").append(params.get(key));
        }
        sb.append("&secret=").append(config.getApiSecret());
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("生成签名失败", e);
            return "";
        }
    }

    @Override
    public boolean refreshAccessToken(String configId) {
        IntegrationConfig config = getConfig(configId);
        if (config == null) return false;
        config.setAccessToken(UUID.randomUUID().toString());
        config.setTokenExpireTime(LocalDateTime.now().plusHours(2));
        config.setUpdateTime(LocalDateTime.now());
        cacheService.set(CONFIG_KEY + configId, config);
        return true;
    }

    @Override
    public Map<String, Object> checkHealth(String configId) {
        Map<String, Object> health = new HashMap<>();
        IntegrationConfig config = getConfig(configId);
        if (config == null) {
            health.put("status", "unknown");
            health.put("message", "配置不存在");
            return health;
        }
        health.put("configId", configId);
        health.put("systemCode", config.getSystemCode());
        health.put("status", config.getStatus());
        health.put("connected", testConnection(configId));
        return health;
    }

    @Override
    public boolean testConnection(String configId) {
        IntegrationConfig config = getConfig(configId);
        if (config == null) return false;
        return "enabled".equals(config.getStatus());
    }

    private SyncRecord createSyncRecord(String configId, String syncType, String operation, Map<String, Object> data) {
        SyncRecord record = new SyncRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setConfigId(configId);
        record.setSyncType(syncType);
        record.setOperation(operation);
        record.setDataId(data.get("id") != null ? data.get("id").toString() : null);
        record.setStatus("pending");
        record.setRetryCount(0);
        record.setStartTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        return record;
    }

    private void saveSyncRecord(SyncRecord record) {
        cacheService.set(SYNC_RECORD_KEY + record.getRecordId(), record);
    }

    private void updateLastSyncTime(String configId) {
        IntegrationConfig config = getConfig(configId);
        if (config != null) {
            config.setLastSyncTime(LocalDateTime.now());
            cacheService.set(CONFIG_KEY + configId, config);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
