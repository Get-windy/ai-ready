package cn.aiedge.base.config;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;

/**
 * 配置变更事件监听器（订阅 {@code sys:config:change}）。
 *
 * <p><b>2026-09-21 死代码清理</b>：本类原先还维护一个静态 {@code LOCAL_CACHE}
 * （外加 {@code getCachedValue} / {@code updateCache} 两个静态方法），但
 * <b>全仓没有任何一处读过它</b>（`grep -rn "getCachedValue|updateCache" backend` 除本类外零命中）
 * —— 也就是一个只写不读的静态 Map：每次配置变更往里塞一份，永远没人取，
 * 既是死代码，也是内存里只增不减的一份残留。已整段删除。</p>
 *
 * <p><b>为什么保留本类</b>：它仍是本进程订阅该频道的唯一入口，
 * 「哪个配置键在什么时候被改了」这条日志在多实例部署下是唯一的跨进程可见性来源。
 * 发布侧在 {@code SysConfigServiceImpl.publishConfigChange}，报文已带上 {@code tenantId}。</p>
 *
 * <p><b>⚠️ 已知缺口（未实现，勿误以为已有）</b>：{@code SysConfigServiceImpl.configCache}
 * 是**进程内**缓存，本监听器<b>并没有</b>去失效它（当初那个删掉的静态缓存也不曾做到）。
 * 单实例部署下这没关系（改配置的进程自己会更新自己的缓存），
 * 一旦多实例部署，A 实例改的配置在 B 实例上要等重启或调 {@code /refresh} 才可见。
 * 真正要做的话得让本监听器回调到 Service 的缓存（注意环依赖）或换 Redis 共享缓存。</p>
 */
@Slf4j
@Component
public class ConfigChangeListener implements MessageListener {

    @Autowired(required = false)
    private RedisMessageListenerContainer redisContainer;

    private static final String CONFIG_CHANGE_CHANNEL = "sys:config:change";

    @PostConstruct
    public void init() {
        if (redisContainer != null) {
            redisContainer.addMessageListener(this, new PatternTopic(CONFIG_CHANGE_CHANNEL));
            log.info("配置变更监听器已启动，订阅频道: {}", CONFIG_CHANGE_CHANNEL);
        } else {
            log.info("Redis未配置，配置变更监听器以本地模式运行");
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            Map<String, Object> data = JSONUtil.toBean(json, Map.class);

            log.info("收到配置变更通知: tenantId={}, key={}, value={}",
                    data.get("tenantId"), data.get("key"), data.get("value"));

        } catch (Exception e) {
            log.error("处理配置变更消息失败: {}", e.getMessage());
        }
    }
}
