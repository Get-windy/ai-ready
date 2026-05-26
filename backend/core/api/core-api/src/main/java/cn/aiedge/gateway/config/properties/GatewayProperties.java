package cn.aiedge.gateway.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关配置属性
 * 配置网关的各种参数和策略
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    /**
     * 路由配置
     */
    private Routes routes = new Routes();

    /**
     * 限流配置
     */
    private RateLimit rateLimit = new RateLimit();

    /**
     * 安全配置
     */
    private Security security = new Security();

    /**
     * 监控配置
     */
    private Monitor monitor = new Monitor();

    // Getters and Setters
    public Routes getRoutes() {
        return routes;
    }

    public void setRoutes(Routes routes) {
        this.routes = routes;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * 路由配置内部类
     */
    public static class Routes {
        private List<RouteConfig> routeConfigs;
        private boolean enableDynamic = true; // 是否启用动态路由

        // Getters and Setters
        public List<RouteConfig> getRouteConfigs() {
            return routeConfigs;
        }

        public void setRouteConfigs(List<RouteConfig> routeConfigs) {
            this.routeConfigs = routeConfigs;
        }

        public boolean isEnableDynamic() {
            return enableDynamic;
        }

        public void setEnableDynamic(boolean enableDynamic) {
            this.enableDynamic = enableDynamic;
        }
    }

    /**
     * 限流配置内部类
     */
    public static class RateLimit {
        private boolean enabled = true; // 是否启用限流
        private int defaultLimit = 100; // 默认每秒请求数
        private int defaultCapacity = 100; // 默认令牌桶容量
        private String keyResolver = "ip"; // 限流键解析器类型

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDefaultLimit() {
            return defaultLimit;
        }

        public void setDefaultLimit(int defaultLimit) {
            this.defaultLimit = defaultLimit;
        }

        public int getDefaultCapacity() {
            return defaultCapacity;
        }

        public void setDefaultCapacity(int defaultCapacity) {
            this.defaultCapacity = defaultCapacity;
        }

        public String getKeyResolver() {
            return keyResolver;
        }

        public void setKeyResolver(String keyResolver) {
            this.keyResolver = keyResolver;
        }
    }

    /**
     * 安全配置内部类
     */
    public static class Security {
        private boolean enableAuth = true; // 是否启用认证
        private boolean enableBlackWhiteList = true; // 是否启用黑白名单
        private List<String> whitelistIps; // 白名单IP列表
        private List<String> blacklistIps; // 黑名单IP列表
        private boolean enableCors = true; // 是否启用CORS

        // Getters and Setters
        public boolean isEnableAuth() {
            return enableAuth;
        }

        public void setEnableAuth(boolean enableAuth) {
            this.enableAuth = enableAuth;
        }

        public boolean isEnableBlackWhiteList() {
            return enableBlackWhiteList;
        }

        public void setEnableBlackWhiteList(boolean enableBlackWhiteList) {
            this.enableBlackWhiteList = enableBlackWhiteList;
        }

        public List<String> getWhitelistIps() {
            return whitelistIps;
        }

        public void setWhitelistIps(List<String> whitelistIps) {
            this.whitelistIps = whitelistIps;
        }

        public List<String> getBlacklistIps() {
            return blacklistIps;
        }

        public void setBlacklistIps(List<String> blacklistIps) {
            this.blacklistIps = blacklistIps;
        }

        public boolean isEnableCors() {
            return enableCors;
        }

        public void setEnableCors(boolean enableCors) {
            this.enableCors = enableCors;
        }
    }

    /**
     * 监控配置内部类
     */
    public static class Monitor {
        private boolean enableAccessLog = true; // 是否启用访问日志
        private boolean enableMetrics = true; // 是否启用指标收集
        private boolean enableTrace = true; // 是否启用链路追踪
        private int logRetentionDays = 30; // 日志保留天数

        // Getters and Setters
        public boolean isEnableAccessLog() {
            return enableAccessLog;
        }

        public void setEnableAccessLog(boolean enableAccessLog) {
            this.enableAccessLog = enableAccessLog;
        }

        public boolean isEnableMetrics() {
            return enableMetrics;
        }

        public void setEnableMetrics(boolean enableMetrics) {
            this.enableMetrics = enableMetrics;
        }

        public boolean isEnableTrace() {
            return enableTrace;
        }

        public void setEnableTrace(boolean enableTrace) {
            this.enableTrace = enableTrace;
        }

        public int getLogRetentionDays() {
            return logRetentionDays;
        }

        public void setLogRetentionDays(int logRetentionDays) {
            this.logRetentionDays = logRetentionDays;
        }
    }

    /**
     * 路由配置内部类
     */
    public static class RouteConfig {
        private String id;
        private String path;
        private String url;
        private String serviceId;
        private List<PredicateConfig> predicates;
        private List<FilterConfig> filters;

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public List<PredicateConfig> getPredicates() {
            return predicates;
        }

        public void setPredicates(List<PredicateConfig> predicates) {
            this.predicates = predicates;
        }

        public List<FilterConfig> getFilters() {
            return filters;
        }

        public void setFilters(List<FilterConfig> filters) {
            this.filters = filters;
        }
    }

    /**
     * 断言配置内部类
     */
    public static class PredicateConfig {
        private String name;
        private String args;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArgs() {
            return args;
        }

        public void setArgs(String args) {
            this.args = args;
        }
    }

    /**
     * 过滤器配置内部类
     */
    public static class FilterConfig {
        private String name;
        private String args;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getArgs() {
            return args;
        }

        public void setArgs(String args) {
            this.args = args;
        }
    }
}
