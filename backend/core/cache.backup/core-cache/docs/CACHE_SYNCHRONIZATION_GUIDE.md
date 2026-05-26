# 缓存同步机制实现指南

## 概述

本文档介绍了AI-Ready项目中实现的缓存同步机制，包括Cache Aside模式、Write Through模式以及缓存穿透、击穿、雪崩的防护措施。

## 架构设计

### 1. Cache Aside模式实现 (`CacheAsideSyncService`)

Cache Aside是最常用的缓存模式，应用程序负责缓存的读写管理。

#### 核心特性：
- 先查缓存，缓存未命中再查数据库
- 数据库查询结果回填缓存
- 支持分布式锁防止缓存击穿
- 处理缓存穿透（缓存空值）
- 缓存同步机制

#### 使用示例：
```java
@Service
public class UserService {
    
    @Autowired
    private CacheAsideSyncService cacheAsideService;
    
    public User getUserById(Long userId) {
        String cacheKey = "user:info:" + userId;
        
        // 使用Cache Aside模式获取用户信息
        return cacheAsideService.getFromCache(
            cacheKey, 
            User.class, 
            () -> userRepository.findById(userId), // 数据库查询逻辑
            3600 // TTL 1小时
        );
    }
}
```

### 2. Write Through模式实现 (`WriteThroughCacheService`)

Write Through模式在写入数据时同时更新缓存和数据库，保证数据一致性。

#### 核心特性：
- 写入时同步更新缓存和数据库
- 支持Write Behind异步写入模式
- 集群环境下的缓存同步
- 异常处理和回滚机制

#### 使用示例：
```java
@Service
public class UserService {
    
    @Autowired
    private WriteThroughCacheService writeThroughService;
    
    public User updateUser(User user) {
        String cacheKey = "user:info:" + user.getId();
        
        // 使用Write Through模式更新用户信息
        return writeThroughService.writeThrough(
            cacheKey,
            user,
            this::saveToDatabase, // 数据库保存逻辑
            3600 // TTL 1小时
        );
    }
    
    private User saveToDatabase(User user) {
        return userRepository.save(user);
    }
}
```

### 3. 缓存保护机制 (`CacheProtectionService`)

针对缓存常见问题的综合解决方案。

#### 3.1 缓存穿透保护
- **问题**：查询不存在的数据，缓存和数据库都没有，每次都查询数据库
- **解决方案**：缓存空值或使用布隆过滤器

```java
// 处理缓存穿透
User user = cacheProtectionService.handleCachePenetration(
    cacheKey,
    () -> userRepository.findById(id), // 数据库查询
    User.class,
    300 // 空值缓存时间
);
```

#### 3.2 缓存击穿保护
- **问题**：热点数据在缓存过期的瞬间，大量请求直接打到数据库
- **解决方案**：分布式锁 + 双重检查

```java
// 处理缓存击穿
User user = cacheProtectionService.handleCacheBreakdown(
    cacheKey,
    () -> userRepository.findById(id),
    User.class,
    3600
);
```

#### 3.3 缓存雪崩保护
- **问题**：大量缓存在同一时间过期，导致大量请求打到数据库
- **解决方案**：设置不同的过期时间、多级缓存、熔断降级

```java
// 处理缓存雪崩（使用随机TTL偏差）
User user = cacheProtectionService.handleCacheavalanche(
    cacheKey,
    () -> userRepository.findById(id),
    User.class,
    3600, // 基础TTL
    600   // 随机偏差（±600秒）
);
```

### 4. 缓存同步机制 (`CacheSyncService`)

在分布式环境下保持缓存一致性。

#### 核心特性：
- 基于Redis Pub/Sub实现
- 支持多种同步操作（EVICT、UPDATE、CLEAR_ALL）
- 节点识别，避免自己发布的消息被自己处理
- 集群环境下的缓存一致性

## 统一接口 (`CacheStrategyUtil`)

提供统一的缓存操作接口，支持多种策略：

```java
@Autowired
private CacheStrategyUtil cacheStrategyUtil;

// 使用统一接口，根据策略自动选择最佳方式
User user = cacheStrategyUtil.getFromCache(
    cacheKey,
    User.class,
    () -> userRepository.findById(id),
    CacheStrategyUtil.CacheStrategy.CACHE_ASIDE_PROTECTED, // 策略
    3600 // TTL
);
```

## 配置选项 (`CacheSyncConfig`)

通过配置文件自定义缓存行为：

```yaml
app:
  cache:
    sync:
      redis:
        sync-channel-prefix: "cache:sync:"
        sync-timeout: 5000
        max-retries: 3
      protection:
        enable-penetration-protection: true
        enable-breakdown-protection: true
        enable-avalanche-protection: true
        empty-value-ttl: 300
      advanced:
        enable-write-through: true
        enable-write-behind: true
        write-behind-delay: 1000
```

## 最佳实践

1. **选择合适的缓存模式**：
   - 读多写少：Cache Aside
   - 强一致性要求：Write Through
   - 高吞吐量写入：Write Behind

2. **合理设置TTL**：
   - 热点数据可设置较长TTL
   - 敏感数据设置较短TTL
   - 使用随机偏差避免缓存雪崩

3. **监控缓存性能**：
   - 缓存命中率
   - 响应时间
   - 内存使用情况

4. **容错处理**：
   - 缓存服务不可用时降级到数据库直查
   - 合理的重试机制
   - 熔断保护

## 集成测试

`CacheIntegrationTest` 包含了完整的集成测试用例，覆盖了各种缓存模式和保护机制的测试。

## 总结

本实现提供了完整的缓存同步解决方案，包括多种缓存模式、全面的保护机制和集群环境下的同步能力，能够有效应对高并发场景下的缓存挑战。