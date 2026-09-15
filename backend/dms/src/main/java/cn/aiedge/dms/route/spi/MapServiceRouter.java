package cn.aiedge.dms.route.spi;

import cn.aiedge.dms.route.service.MapKeyResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 地图服务路由器
 *
 * 按 {@link MapKeyResolver} 解析出的默认服务商选择实现；所选服务商**未配置 Key 时自动降级**
 * 到 {@link LocalMapService}（直线模式 + 本地几何围栏），避免因缺少 Key 导致路线规划、距离计算
 * 等功能整体不可用。Key 支持热更新：环境变量 / 配置文件 / 《配送参数》配置中心任一来源变更，
 * 都会在缓存 TTL（30 秒，或配置保存时立即）内反映到服务实例上。
 *
 * @author AI-Ready Team
 */
@Slf4j
@Component
public class MapServiceRouter {

    private final Map<String, MapService> services = new LinkedHashMap<>();
    private final MapKeyResolver keyResolver;

    public MapServiceRouter(List<MapService> mapServices, MapKeyResolver keyResolver) {
        this.keyResolver = keyResolver;
        for (MapService service : mapServices) {
            services.put(service.getProvider(), service);
        }
        log.info("[MapServiceRouter] 可用地图服务商={}", services.keySet());
    }

    /**
     * 当前生效的地图服务（未配置 Key 时返回本地兜底实现）
     */
    public MapService current() {
        MapKeyResolver.Snapshot snapshot = keyResolver.resolve();
        String provider = snapshot.defaultProvider();
        MapService selected = services.get(provider);
        if (selected == null) {
            log.warn("[MapServiceRouter] 未知地图服务商 {}，回退本地直线模式", provider);
            return services.get("local");
        }
        MapKeyResolver.ProviderKey providerKey = snapshot.keys().get(provider);
        if (providerKey != null && providerKey.configured()) {
            // 把生效 Key 同步到服务实例（来源可能是环境变量/配置文件/配置中心，且可能刚变更）
            selected.applyApiKey(providerKey.apiKey());
            return selected;
        }
        // Key 被撤销时清空实例内的旧 Key，避免继续按旧配置外呼
        selected.applyApiKey("");
        return services.getOrDefault("local", selected);
    }

    /**
     * 是否处于降级模式（外部地图服务不可用）
     */
    public boolean degraded() {
        MapService current = current();
        return current == null || "local".equals(current.getProvider());
    }

    /**
     * 服务商配置状态（供《路线规划》页展示与《配送参数》指引）
     */
    public List<Map<String, Object>> providerStatus() {
        MapKeyResolver.Snapshot snapshot = keyResolver.resolve();
        MapService active = current();
        return snapshot.keys().values().stream()
                .map(key -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("provider", key.provider());
                    item.put("configured", key.configured());
                    item.put("active", active != null && key.provider().equals(active.getProvider()));
                    item.put("source", key.source().name());
                    item.put("envVarName", keyResolver.envVarName(key.provider()));
                    item.put("configKey", keyResolver.configKeyName(key.provider()));
                    return item;
                })
                .toList();
    }

    /** 当前默认服务商（配置解析结果，可能来自配置中心） */
    public String getDefaultProvider() {
        return keyResolver.defaultProvider();
    }
}
