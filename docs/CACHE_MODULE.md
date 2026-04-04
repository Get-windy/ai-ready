# AI-Ready 缓存模块文档

## 概述

AI-Ready 缓存模块提供统一的缓存管理能力，基于 Redis 实现，支持多种过期策略和统计监控。

## 模块结构

```
cn.aiedge.cache
├── config/                    # 配置类
│   ├── CacheConfig.java       # Redis缓存配置
│   └── CacheNames.java        # 缓存名称常量
├── service/                   # 服务类
│   ├── CacheService.java      # 核心缓存服务
│   └── CacheEvictService.java # 缓存失效管理
├── policy/                    # 过期策略
│   ├── CacheExpirationPolicy.java      # 过期策略接口
│   ├── SimpleExpirationPolicy.java     # 简单固定过期
│   ├── SlidingExpirationPolicy.java    # 滑动过期
│   └── RefreshAheadPolicy.java         # 预刷新过期
├── builder/                   # 工具类
│   └── CacheKeyBuilder.java   # 缓存键构建器
└── stats/                     # 统计监控
    └── CacheStatistics.java   # 缓存统计
```

## 核心功能

### 1. CacheService - 核心缓存服务

提供完整的 Redis 操作封装：

```java
@Autowired
private CacheService cacheService;

// 基础操作
cacheService.set("key", "value");
cacheService.set("key", "value", 60, TimeUnit.SECONDS);
String value = cacheService.get("key");
cacheService.delete("key");

// 自增/自减
Long count = cacheService.increment("counter");
Long count = cacheService.increment("counter", 10);

// Hash操作
cacheService.hSet("hash:key", "field", "value");
String value = cacheService.hGet("hash:key", "field");
Map<Object, Object> map = cacheService.hGetAll("hash:key");

// List操作
cacheService.lPush("list:key", "value");
cacheService.rPush("list:key", "value");
List<Object> list = cacheService.lRange("list:key", 0, -1);

// Set操作
cacheService.sAdd("set:key", "value1", "value2");
Set<Object> members = cacheService.sMembers("set:key");

// ZSet操作
cacheService.zAdd("zset:key", "member", 100.0);
Set<Object> range = cacheService.zRange("zset:key", 0, 10);

// 模式匹配
Long deleted = cacheService.deleteByPattern("user:*");
Set<String> keys = cacheService.keys("product:*");
```

### 2. CacheEvictService - 缓存失效管理

提供语义化的缓存清除方法：

```java
@Autowired
private CacheEvictService cacheEvictService;

// 用户相关缓存清除
cacheEvictService.evictUserInfo(userId);
cacheEvictService.evictUserPermissions(userId);
cacheEvictService.evictUserAll(userId);

// 业务数据缓存清除
cacheEvictService.evictProduct(productId);
cacheEvictService.evictAllProducts();
cacheEvictService.evictStock(productId, warehouseId);

// 系统缓存清除
cacheEvictService.evictSysConfig("app.name");
cacheEvictService.evictDict("status");

// 批量清除
cacheEvictService.evictAllBusiness();
cacheEvictService.evictAll();
```

### 3. 过期策略

#### 简单固定过期 (SimpleExpirationPolicy)

缓存固定时间后过期：

```java
// 使用预定义策略
SimpleExpirationPolicy.SHORT    // 5分钟
SimpleExpirationPolicy.MEDIUM   // 30分钟
SimpleExpirationPolicy.LONG     // 2小时

// 自定义过期时间
SimpleExpirationPolicy policy = new SimpleExpirationPolicy(1800);
```

#### 滑动过期 (SlidingExpirationPolicy)

每次访问缓存时自动延长过期时间：

```java
// 使用预定义策略
SlidingExpirationPolicy.USER_SESSION  // 30分钟无活动过期
SlidingExpirationPolicy.USER_TOKEN    // 1小时无活动过期

// 自定义滑动过期
SlidingExpirationPolicy policy = new SlidingExpirationPolicy(1800);
```

#### 预刷新过期 (RefreshAheadPolicy)

在缓存过期前自动异步刷新：

```java
// 使用预定义策略
RefreshAheadPolicy.CONFIG  // 1小时TTL，75%刷新
RefreshAheadPolicy.DICT    // 2小时TTL，80%刷新

// 自定义预刷新
RefreshAheadPolicy policy = new RefreshAheadPolicy(3600, 0.75);
```

### 4. CacheKeyBuilder - 缓存键构建器

提供流畅的 API 构建缓存键：

```java
// 基本用法
String key = CacheKeyBuilder.create()
    .append("user")
    .append(userId)
    .build();
// 结果: "ai-ready:user:123"

// 条件追加
String key = CacheKeyBuilder.create()
    .append("product")
    .appendIf(hasCategory, "category")
    .build();

// 预定义方法
CacheKeyBuilder.userKey(123L);              // "ai-ready:user:123"
CacheKeyBuilder.userPermissionsKey(123L);   // "ai-ready:user:123:permissions"
CacheKeyBuilder.productKey(456L);           // "ai-ready:product:456"
CacheKeyBuilder.stockKey(100L, 1L);         // "ai-ready:stock:1:100"
```

### 5. CacheStatistics - 缓存统计

跟踪缓存命中率等指标：

```java
@Autowired
private CacheStatistics cacheStatistics;

// 记录统计
cacheStatistics.recordHit("user:info");
cacheStatistics.recordMiss("user:info");

// 获取命中率
double hitRate = cacheStatistics.getHitRate("user:info");

// 获取统计摘要
CacheStatsSummary summary = cacheStatistics.getSummary("user:info");
System.out.println(summary); // CacheStats[user:info]: hits=10, misses=2, hitRate=83.33%
```

## 缓存命名规范

| 缓存类型 | 前缀 | 示例 | TTL |
|---------|------|------|-----|
| 用户信息 | user:info | user:info:123 | 30分钟 |
| 用户权限 | user:perms | user:perms:123 | 2小时 |
| 产品数据 | product | product:456 | 10分钟 |
| 客户数据 | customer | customer:789 | 10分钟 |
| 订单数据 | order | order:101 | 10分钟 |
| 库存数据 | stock | stock:1:100 | 5分钟 |
| 系统配置 | sys:config | sys:config:app.name | 1小时 |
| 字典数据 | dict | dict:status | 1小时 |

## 最佳实践

### 1. 缓存穿透防护

```java
// 缓存空值，防止缓存穿透
public User getUserById(Long id) {
    String key = CacheKeyBuilder.userKey(id);
    User user = cacheService.get(key);
    if (user != null) {
        return user;
    }
    
    user = userRepository.findById(id);
    if (user == null) {
        // 缓存空值，短过期时间
        cacheService.setEx(key, new User(), 60);
        return null;
    }
    
    cacheService.setEx(key, user, 1800);
    return user;
}
```

### 2. 缓存雪崩防护

```java
// 使用随机过期时间，避免同时失效
int baseTtl = 3600;
int randomOffset = new Random().nextInt(300);
cacheService.setEx(key, value, baseTtl + randomOffset);

// 或使用预刷新策略
RefreshAheadPolicy policy = new RefreshAheadPolicy(3600, 0.75);
```

### 3. 数据一致性

```java
@Transactional
public void updateUser(User user) {
    userRepository.save(user);
    // 更新后立即清除缓存
    cacheEvictService.evictUserAll(user.getId());
}
```

### 4. 批量操作优化

```java
// 批量删除优于循环单个删除
cacheEvictService.evictAllProducts();
// 而不是
// for (Long id : productIds) { cacheEvictService.evictProduct(id); }
```

## 配置说明

### application.yml

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

## 单元测试

模块包含完整的单元测试：

- `CacheServiceTest` - 核心缓存服务测试
- `CacheEvictServiceTest` - 缓存失效管理测试
- `CacheExpirationPolicyTest` - 过期策略测试
- `CacheKeyBuilderTest` - 键构建器测试
- `CacheStatisticsTest` - 统计监控测试

运行测试：

```bash
mvn test -Dtest=CacheServiceTest,CacheEvictServiceTest,CacheExpirationPolicyTest,CacheKeyBuilderTest,CacheStatisticsTest
```

## 版本历史

- v1.0.0 (2026-04-03)
  - 初始版本
  - 支持 Redis 缓存操作
  - 实现三种过期策略
  - 添加缓存统计监控
  - 完整单元测试覆盖
