package cn.aiedge.common.ratelimit;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "ai-ready.rate-limit")
public class AiReadyRateLimitConfig {

    public boolean isEnabled() { return enabled; }
    public int getDefaultQps() { return defaultQps; }
    public int getDefaultCapacity() { return defaultCapacity; }
    public long getDefaultTimeout() { return defaultTimeout; }
    public int getIpQps() { return ipQps; }
    public int getUserQps() { return userQps; }
    public Map<String, ApiLimit> getApiLimits() { return apiLimits; }

    private boolean enabled = true;
    private int defaultQps = 100;
    private int defaultCapacity = 200;
    private long defaultTimeout = 0L;
    private int ipQps = 50;
    private int userQps = 100;
    private Map<String, ApiLimit> apiLimits = new HashMap<>();

    @Data
    public static class ApiLimit {
        private int qps = 100;
        private int capacity = 200;
        private long timeout = 0;
    }
}