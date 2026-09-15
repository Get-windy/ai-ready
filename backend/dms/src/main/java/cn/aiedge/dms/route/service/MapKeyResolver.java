package cn.aiedge.dms.route.service;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.dms.common.constant.DmsConstants;
import cn.aiedge.dms.config.entity.DmsConfig;
import cn.aiedge.dms.config.event.ConfigChangedEvent;
import cn.aiedge.dms.config.mapper.DmsConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 地图服务 Key 解析器（配置落位的唯一读取口径）
 *
 * 读取优先级（高 → 低）：
 * 1. `ENV`    环境变量（`AMAP_API_KEY` / `TENCENT_MAP_API_KEY` / `BAIDU_MAP_API_KEY`）—— 生产推荐；
 * 2. `SPRING` Spring 配置（core-api 的 application.yml / 外部化配置文件 / 启动参数 `--dms.map.*`）；
 * 3. `CONFIG` 数据库配置中心《配送参数》（`dms_config`：全局默认 tenant_id=0 + 租户覆盖）；
 * 4. `NONE`   未配置 → 由 {@code MapServiceRouter} 降级为本地直线模式。
 *
 * 缓存 30 秒；配置中心保存后经 {@link ConfigChangedEvent} 立即失效（本实例即时空生效，
 * 多实例部署时其它实例最迟 30 秒内自动刷新）。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component
public class MapKeyResolver implements MessageListener, SmartInitializingSingleton {

    /** Key 来源 */
    public enum Source {
        /** 环境变量 */
        ENV,
        /** Spring 配置（application.yml / 外部化配置文件 / 启动参数） */
        SPRING,
        /** 数据库配置中心《配送参数》 */
        CONFIG,
        /** 未配置 */
        NONE
    }

    /** 支持的地图服务商与对应的环境变量名 */
    private static final Map<String, String> ENV_VARS = new LinkedHashMap<>() {{
        put("amap", "AMAP_API_KEY");
        put("tencent", "TENCENT_MAP_API_KEY");
        put("baidu", "BAIDU_MAP_API_KEY");
    }};

    /** 数据库配置键（《配送参数》页面可编辑） */
    private static final String CONFIG_KEY_PREFIX = "map.";
    private static final String CONFIG_KEY_SUFFIX = ".api-key";
    private static final String CONFIG_KEY_PROVIDER = "map.default-provider";

    /** 前端底图 JS SDK Key 的配置键/环境变量 */
    private static final String JS_KEY_CONFIG_KEY = "map.amap.js-key";
    private static final String JS_KEY_ENV = "AMAP_JS_KEY";

    /** 缓存有效期：30 秒（跨实例自动感知配置中心变更） */
    private static final long CACHE_TTL_MILLIS = 30_000L;

    private final DmsConfigMapper dmsConfigMapper;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final GeocodeCache geocodeCache;

    public MapKeyResolver(DmsConfigMapper dmsConfigMapper,
                          ObjectProvider<StringRedisTemplate> redisProvider,
                          GeocodeCache geocodeCache) {
        this.dmsConfigMapper = dmsConfigMapper;
        this.redisProvider = redisProvider;
        this.geocodeCache = geocodeCache;
    }

    /**
     * 订阅配置变更频道：任一实例保存配置后，本实例缓存立即失效（跨实例即时生效）
     *
     * 订阅方式与平台既有 `PermissionChangeRedisListener` 保持一致（直接用
     * `StringRedisTemplate.execute(connection -> connection.subscribe(...))`）——
     * 本项目**没有** `RedisMessageListenerContainer` bean，用容器 API 只会静默拿不到
     * （core-base 的 ConfigChangeListener 就是因此打印「Redis未配置」）。
     * 订阅放在 `afterSingletonsInstantiated`：确保所有单例（含 Redis 客户端）已就绪。
     */
    @Override
    public void afterSingletonsInstantiated() {
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null || redis.getConnectionFactory() == null) {
            log.info("[MapKeyResolver] Redis 未启用，配置变更走 30 秒 TTL 自动刷新");
            return;
        }
        try {
            // 用**独立连接**订阅：StringRedisTemplate.execute(...) 的回调返回后会归还连接，
            // 订阅随之中断（平台既有 PermissionChangeRedisListener 即为此写法，收不到消息）。
            // RedisConnection.subscribe 会向连接工厂申请专用订阅连接并常驻，故这里持有引用不关闭。
            this.subscriptionConnection = redis.getConnectionFactory().getConnection();
            this.subscriptionConnection.subscribe(this, DmsConstants.CONFIG_CHANGE_CHANNEL.getBytes(StandardCharsets.UTF_8));
            log.info("[MapKeyResolver] 已订阅配置变更频道: {}", DmsConstants.CONFIG_CHANGE_CHANNEL);
        } catch (Exception e) {
            log.warn("[MapKeyResolver] 订阅配置变更频道失败，跨实例变更回退 30 秒 TTL: {}", e.getMessage());
        }
    }

    /** Redis 广播回调：仅处理地图相关键 */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        if (body.contains(CONFIG_KEY_PREFIX)) {
            invalidate();
            log.info("[MapKeyResolver] 收到跨实例配置变更，缓存已失效: {}", body);
        }
    }

    @Value("${dms.map.default-provider:amap}")
    private String springDefaultProvider;

    @Value("${dms.map.amap.api-key:}")
    private String springAmapKey;

    @Value("${dms.map.tencent.api-key:}")
    private String springTencentKey;

    @Value("${dms.map.baidu.api-key:}")
    private String springBaiduKey;

    /** 前端底图用 JS SDK Key（与 Web 服务 Key 不同，可公开但需域名限制） */
    @Value("${dms.map.amap.js-key:}")
    private String springAmapJsKey;

    private volatile long cacheExpireAt = 0L;
    private volatile Snapshot snapshot;

    /** 常驻的订阅连接（不可关闭，否则订阅失效） */
    private RedisConnection subscriptionConnection;

    /** 解析结果快照 */
    public record Snapshot(String defaultProvider, Map<String, ProviderKey> keys, String jsKey) {
    }

    /** 单个服务商的生效 Key */
    public record ProviderKey(String provider, String apiKey, Source source) {
        public boolean configured() {
            return StringUtils.hasText(apiKey);
        }
    }

    /**
     * 解析当前生效的地图配置（带 30 秒缓存）
     */
    public Snapshot resolve() {
        long now = System.currentTimeMillis();
        if (snapshot == null || now >= cacheExpireAt) {
            synchronized (this) {
                if (snapshot == null || now >= cacheExpireAt) {
                    snapshot = load();
                    cacheExpireAt = System.currentTimeMillis() + CACHE_TTL_MILLIS;
                }
            }
        }
        return snapshot;
    }

    /** 指定服务商的生效 Key */
    public ProviderKey key(String provider) {
        return resolve().keys().getOrDefault(provider,
                new ProviderKey(provider, "", Source.NONE));
    }

    /** 当前默认服务商（环境变量 → Spring → 配置中心 → amap） */
    public String defaultProvider() {
        return resolve().defaultProvider();
    }

    /** 环境变量名（供页面提示） */
    public String envVarName(String provider) {
        return ENV_VARS.getOrDefault(provider, "");
    }

    /** 配置中心键名（供页面提示） */
    public String configKeyName(String provider) {
        return CONFIG_KEY_PREFIX + provider + CONFIG_KEY_SUFFIX;
    }

    /** 配置中心「默认服务商」键名 */
    public String defaultProviderConfigKey() {
        return CONFIG_KEY_PROVIDER;
    }

    /** 前端底图用高德 JS SDK Key（来源：环境变量 AMAP_JS_KEY > Spring 配置 > 配置中心 map.amap.js-key） */
    public String jsKey() {
        String env = System.getenv(JS_KEY_ENV);
        if (StringUtils.hasText(env)) {
            return env.trim();
        }
        if (StringUtils.hasText(springAmapJsKey)) {
            return springAmapJsKey.trim();
        }
        Snapshot snapshot = resolve();
        String value = loadFromConfigCenter().get(JS_KEY_CONFIG_KEY);
        if (value == null) {
            value = snapshot.jsKey();
        }
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /** 手动失效（配置保存后由事件触发） */
    public void invalidate() {
        cacheExpireAt = 0L;
    }

    /** 配置中心变更 → 立刻让缓存失效，实现「保存即生效」 */
    @EventListener
    public void onConfigChanged(ConfigChangedEvent event) {
        if (event != null && event.configKey() != null && event.configKey().startsWith(CONFIG_KEY_PREFIX)) {
            invalidate();
            // 换 Key / 切服务商：地理编码缓存整体失效（旧结果可能来自另一服务商/坐标系）
            if (event.configKey().endsWith(CONFIG_KEY_SUFFIX) || event.configKey().equals(CONFIG_KEY_PROVIDER)) {
                geocodeCache.clearAll();
            }
            log.info("[MapKeyResolver] 地图配置已变更，缓存失效: {}", event.configKey());
        }
    }

    // ==================== 私有方法 ====================

    private Snapshot load() {
        Map<String, String> dbValues = loadFromConfigCenter();

        Map<String, ProviderKey> keys = new LinkedHashMap<>();
        for (String provider : ENV_VARS.keySet()) {
            keys.put(provider, resolveKey(provider, dbValues));
        }

        String provider = firstNonBlank(
                dbValues.get(CONFIG_KEY_PROVIDER),
                springDefaultProvider,
                "amap");
        return new Snapshot(provider, keys, dbValues.get(JS_KEY_CONFIG_KEY));
    }

    /** 单个服务商：环境变量 > Spring 配置 > 配置中心 */
    private ProviderKey resolveKey(String provider, Map<String, String> dbValues) {
        String envVar = ENV_VARS.get(provider);
        String envValue = System.getenv(envVar);
        if (StringUtils.hasText(envValue)) {
            return new ProviderKey(provider, envValue.trim(), Source.ENV);
        }
        String springValue = springKey(provider);
        if (StringUtils.hasText(springValue)) {
            return new ProviderKey(provider, springValue.trim(), Source.SPRING);
        }
        String dbValue = dbValues.get(CONFIG_KEY_PREFIX + provider + CONFIG_KEY_SUFFIX);
        if (StringUtils.hasText(dbValue)) {
            return new ProviderKey(provider, dbValue.trim(), Source.CONFIG);
        }
        return new ProviderKey(provider, "", Source.NONE);
    }

    private String springKey(String provider) {
        return switch (provider) {
            case "tencent" -> springTencentKey;
            case "baidu" -> springBaiduKey;
            default -> springAmapKey;
        };
    }

    /** 读取配置中心（全局默认 + 当前租户覆盖）；任何异常都不得中断地理能力 */
    private Map<String, String> loadFromConfigCenter() {
        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (DmsConfig config : dmsConfigMapper.selectGlobalConfigs()) {
                if (config.getConfigKey() != null && config.getConfigKey().startsWith(CONFIG_KEY_PREFIX)) {
                    values.put(config.getConfigKey(), config.getConfigValue());
                }
            }
            Long tenantId = SecurityUtils.getCurrentTenantId();
            if (tenantId != null && tenantId != 0L) {
                for (DmsConfig config : dmsConfigMapper.selectByTenantId(tenantId)) {
                    if (config.getConfigKey() != null && config.getConfigKey().startsWith(CONFIG_KEY_PREFIX)) {
                        values.put(config.getConfigKey(), config.getConfigValue());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[MapKeyResolver] 读取配置中心失败，改用环境变量/配置文件: {}", e.getMessage());
        }
        return values;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return "amap";
    }
}
