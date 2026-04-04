# AI-Ready 缓存模块文档

## 概述

本模块提供统一的 Redis 缓存管理能力，包括缓存配置、读写接口、失效策略等。

## 模块结构

```
cn.aiedge.cache
├── config/
│   ├── CacheConfig.java      # 缓存配置类
│   └── CacheNames.java       # 缓存名称常量
└── service/
    ├── CacheService.java     # 缓存读写服务
    └── CacheEvictService.java # 缓存失效服务
```

## 快速开始

### 1. 基础缓存操作

```java
@Autowired
private CacheService cacheService;

// 设置缓存
cacheService.set("user:1001", userInfo);

// 设置缓存（带过期时间）
cacheService.setEx("user:1001", userInfo, 3600); // 1小时

// 获取缓存
UserInfo user = cacheService.get("user:1001", UserInfo.class);

// 删除缓存
cacheService.delete("user:1001");

// 判断是否存在
boolean exists = cacheService.hasKey("user:1001");
```

### 2. Hash 操作

```java
// 设置 Hash 字段
cacheService.hSet("product:1001", "name", "iPhone 15");
cacheService.hSet("product:1001", "price", 5999.00);

// 批量设置
Map<String, Object> productData = new HashMap<>();
productData.put("name", "iPhone 15");
productData.put("price", 5999.00);
productData.put("stock", 100);
cacheService.hSetAll("product:1001", productData);

// 获取所有字段
Map<Object, Object> data = cacheService.hGetAll("product:1001");
```

### 3. 缓存失效

```java
@Autowired
private CacheEvictService cacheEvictService;

// 清除单个用户缓存
cacheEvictService.evictUserInfo(userId);

// 清除用户所有相关缓存
cacheEvictService.evictUserAll(userId);

// 清除产品缓存
cacheEvictService.evictProduct(productId);

// 清除所有产品缓存
cacheEvictService.evictAllProducts();
```

## 缓存策略

### 缓存分层

| 缓存类型 | 过期时间 | 用途 |
|----------|----------|------|
| 用户信息 | 30分钟 | 高频访问，实时性要求高 |
| 用户权限 | 2小时 | 较少变更，可较长缓存 |
| 业务数据 | 10分钟 | 实时性要求一般 |
| 系统配置 | 1小时 | 极少变更 |

### 缓存键命名规范

```
{业务模块}:{实体类型}:{实体ID}
{业务模块}:{实体类型}:{属性}:{ID}

示例:
- user:info:1001          # 用户信息
- user:perms:1001         # 用户权限
- product:2001            # 产品数据
- stock:100:2001          # 仓库ID:产品ID 的库存
```

### 失效策略

1. **主动失效**: 数据变更时立即清除相关缓存
2. **定时失效**: 设置合理的 TTL 过期时间
3. **LRU淘汰**: Redis 配置最大内存，自动淘汰冷数据

## 使用注解

### @Cacheable

```java
@Cacheable(value = CacheNames.USER_INFO, key = "#userId")
public UserInfo getUserById(Long userId) {
    return userMapper.selectById(userId);
}
```

### @CacheEvict

```java
@CacheEvict(value = CacheNames.USER_INFO, key = "#userId")
public void updateUser(Long userId, UserInfo userInfo) {
    userMapper.updateById(userInfo);
}
```

### @CachePut

```java
@CachePut(value = CacheNames.USER_INFO, key = "#userId")
public UserInfo saveUser(Long userId, UserInfo userInfo) {
    userMapper.insert(userInfo);
    return userInfo;
}
```

## 配置说明

### application.yml

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 10000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0
```

## 最佳实践

1. **选择合适的过期时间**
   - 高频变化数据: 短时间 (5-10分钟)
   - 低频变化数据: 较长时间 (1-2小时)
   - 配置类数据: 长时间 (1天或永不过期)

2. **避免缓存穿透**
   - 缓存空值，设置较短过期时间
   - 使用布隆过滤器

3. **避免缓存雪崩**
   - 设置随机过期时间
   - 使用互斥锁

4. **避免缓存击穿**
   - 使用分布式锁
   - 设置热点数据永不过期

5. **缓存数据一致性**
   - 数据变更时主动清除缓存
   - 使用消息队列同步更新

## 性能优化

1. **批量操作**: 使用 Pipeline 或 Lua 脚本
2. **序列化**: 使用 JSON 序列化，便于调试
3. **连接池**: 合理配置 Lettuce 连接池参数
4. **键空间通知**: 用于分布式缓存失效