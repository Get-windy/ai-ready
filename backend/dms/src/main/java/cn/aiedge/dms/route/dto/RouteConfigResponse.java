package cn.aiedge.dms.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 地理能力配置状态（供《路线规划》页展示「Key 配在哪、当前是否降级」的诊断信息）
 *
 * 配置落位（优先级从高到低）：环境变量 → Spring 配置（application.yml / 外部化配置文件 / 启动参数）
 * → 数据库配置中心《配送参数》→ 内置默认（直线降级模式）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteConfigResponse {

    /** 当前生效的服务商：amap/tencent/baidu/local */
    private String provider;

    /** 解析出的默认服务商（可能来自《配送参数》map.default-provider） */
    private String defaultProvider;

    /** 当前服务商是否已配置 Key */
    private boolean configured;

    /** 是否处于降级模式（直线距离 + 本地几何围栏） */
    private boolean degraded;

    /** Key 来源：ENV-环境变量 SPRING-Spring配置 CONFIG-配置中心 NONE-未配置 */
    private String keySource;

    /** 该服务商对应的环境变量名（如 AMAP_API_KEY） */
    private String envVarName;

    /** 该服务商在《配送参数》中的配置键（如 map.amap.api-key） */
    private String configKey;

    /** 人类可读的配置指引（按当前来源给出下一步操作） */
    private String hint;

    /**
     * 前端底图用高德 **JS SDK Key**（明文返回：JS Key 设计上即公开、需按域名限制）
     *
     * 与 Web 服务 Key（`map.amap.api-key`）是两把不同的 Key，切勿混用。
     * 留空时前端回退自研矢量画布。
     */
    private String jsKey;

    /** JS SDK Key 是否已配置 */
    private boolean jsKeyConfigured;

    /** 各服务商配置状态（provider/configured/active/source/envVarName/configKey） */
    private List<Map<String, Object>> providers;

    /** 统一坐标体系 */
    private String coordSystem;

    /** 支持的出行方式 */
    private List<String> travelModes;

    /** 地理编码缓存条数 */
    private int geocodeCacheSize;
}
