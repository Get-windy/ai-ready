package cn.aiedge.cache.demo;

import cn.aiedge.cache.service.*;
import cn.aiedge.cache.util.CacheStrategyUtil;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 缓存同步机制演示程序
 * 演示Cache Aside、Write Through模式及缓存保护机制的使用
 *
 * @author AI-Ready Team
 */
@Slf4j
public class CacheSyncDemo {

    public static void main(String[] args) {
        log.info("开始演示缓存同步机制");

        // 这里我们只做概念演示，实际使用时会通过Spring容器获取服务
        demonstrateCacheUsage();
    }

    private static void demonstrateCacheUsage() {
        log.info("=== 演示Cache Aside模式 ===");
        
        // 模拟用户数据
        User userData = new User(1L, "张三", "zhangsan@example.com");
        String cacheKey = "user:info:1";

        // 1. Cache Aside - 先查缓存，缓存未命中再查数据库
        log.info("1. 模拟第一次访问（缓存未命中）");
        log.info("   - 缓存中查找: {}", cacheKey);
        log.info("   - 缓存未命中，查询数据库...");
        log.info("   - 数据库查询结果: {}", JSONUtil.toJsonStr(userData));
        log.info("   - 将结果写入缓存，TTL: 3600秒");

        log.info("\n2. 模拟第二次访问（缓存命中）");
        log.info("   - 缓存中查找: {}", cacheKey);
        log.info("   - 缓存命中，返回数据，无需查询数据库");

        // 3. Cache Aside - 更新数据
        log.info("\n3. 模拟数据更新");
        log.info("   - 更新数据库...");
        log.info("   - 删除缓存: {}", cacheKey);
        log.info("   - 下次访问将重新加载最新数据");

        log.info("\n=== 演示Write Through模式 ===");
        
        User newUser = new User(2L, "李四", "lisi@example.com");
        String newCacheKey = "user:info:2";

        log.info("1. Write Through写入");
        log.info("   - 同时写入缓存和数据库: {}", newCacheKey);
        log.info("   - 保证缓存和数据库数据一致性");

        log.info("\n=== 演示缓存保护机制 ===");

        String nonExistentKey = "user:info:999";

        log.info("1. 缓存穿透保护");
        log.info("   - 查询不存在的数据: {}", nonExistentKey);
        log.info("   - 首次查询数据库返回null");
        log.info("   - 将空值缓存一段时间（防止重复查询数据库）");

        log.info("\n2. 缓存击穿保护");
        log.info("   - 热点数据在缓存过期时，使用分布式锁");
        log.info("   - 只允许一个请求查询数据库，其他请求等待");
        log.info("   - 避免大量请求直接打到数据库");

        log.info("\n3. 缓存雪崩保护");
        log.info("   - 设置不同缓存TTL，避免同时过期");
        log.info("   - 使用多级缓存策略");
        log.info("   - 实施熔断降级机制");

        log.info("\n=== 演示缓存同步机制 ===");
        
        log.info("1. 集群环境下缓存同步");
        log.info("   - 节点A更新数据，发布同步消息");
        log.info("   - 节点B/B/C... 接收同步消息");
        log.info("   - 清除本地缓存，保持数据一致性");

        log.info("\n缓存同步机制演示完成！");
    }

    @Data
    private static class User {
        private Long id;
        private String name;
        private String email;

        public User(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }
}