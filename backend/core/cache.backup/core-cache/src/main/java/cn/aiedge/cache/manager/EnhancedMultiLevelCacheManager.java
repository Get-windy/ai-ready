package cn.aiedge.cache.manager;

import cn.aiedge.cache.annotation.MultiLevelCache;
import cn.aiedge.cache.annotation.MultiLevelCacheEvict;
import cn.aiedge.cache.config.CacheConfigEnhanced;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 增强版多级缓存管理器
 * 支持动态缓存策略配置和更精细的缓存控制
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Aspect
@Component
public class EnhancedMultiLevelCacheManager {

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, Cache<String, Object>> localCaches = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    public EnhancedMultiLevelCacheManager(StringRedisTemplate redisTemplate,
                                         @Resource(name = "cacheExecutor") ScheduledExecutorService scheduler) {
        this.redisTemplate = redisTemplate;
        this.scheduler = scheduler;
    }

    /**
     * 增强版多级缓存拦截器
     */
    @Around("@annotation(cn.aiedge.cache.annotation.MultiLevelCache)")
    public Object aroundCache(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        MultiLevelCache cacheAnnotation = method.getAnnotation(MultiLevelCache.class);

        // 解析缓存Key
        String cacheKey = parseKey(cacheAnnotation.key(), point);
        String cacheName = StrUtil.isNotBlank(cacheAnnotation.value()) ?
                cacheAnnotation.value() : method.getDeclaringClass().getSimpleName();
        String fullKey = cacheName + ":" + cacheKey;

        // 检查条件
        if (StrUtil.isNotBlank(cacheAnnotation.condition())) {
            boolean condition = parseCondition(cacheAnnotation.condition(), point);
            if (!condition) {
                return point.proceed();
            }
        }

        // 1. 查询本地缓存
        if (cacheAnnotation.enableLocal()) {
            Cache<String, Object> localCache = getLocalCache(cacheName, cacheAnnotation);
            Object localValue = localCache.getIfPresent(fullKey);
            if (localValue != null) {
                log.debug("本地缓存命中: key={}", fullKey);
                return localValue;
            }
        }

        // 2. 查询Redis缓存
        if (cacheAnnotation.enableRedis()) {
            String redisValue = redisTemplate.opsForValue().get(fullKey);
            if (StrUtil.isNotBlank(redisValue)) {
                Object value = cn.hutool.json.JSONUtil.toBean(redisValue, method.getReturnType());
                log.debug("Redis缓存命中: key={}", fullKey);

                // 回填本地缓存
                if (cacheAnnotation.enableLocal()) {
                    Cache<String, Object> localCache = getLocalCache(cacheName, cacheAnnotation);
                    localCache.put(fullKey, value);
                }
                return value;
            }
        }

        // 3. 缓存未命中，执行方法
        Object result;
        if (cacheAnnotation.sync()) {
            // 同步加载，防止缓存击穿
            result = loadWithLock(fullKey, cacheAnnotation, point);
        } else {
            result = point.proceed();
        }

        // 4. 写入缓存
        if (result != null) {
            writeCache(fullKey, result, cacheAnnotation);
            
            // 如果启用了预刷新，则调度预刷新任务
            if (cacheAnnotation.enableRefreshAhead()) {
                scheduleRefreshAhead(fullKey, cacheAnnotation, point);
            }
        }

        return result;
    }

    /**
     * 缓存清除拦截器（增强版）
     */
    @Around("@annotation(cn.aiedge.cache.annotation.MultiLevelCacheEvict)")
    public Object aroundEvict(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        MultiLevelCacheEvict evictAnnotation = method.getAnnotation(MultiLevelCacheEvict.class);

        // 解析缓存Key
        String cacheKey = parseKey(evictAnnotation.key(), point);
        String cacheName = StrUtil.isNotBlank(evictAnnotation.value()) ?
                evictAnnotation.value() : method.getDeclaringClass().getSimpleName();
        String fullKey = cacheName + ":" + cacheKey;

        // 检查条件
        if (StrUtil.isNotBlank(evictAnnotation.condition())) {
            boolean condition = parseCondition(evictAnnotation.condition(), point);
            if (!condition) {
                return point.proceed();
            }
        }

        // 执行方法
        Object result = point.proceed();

        // 清除缓存
        if (evictAnnotation.beforeInvocation()) {
            evictCache(fullKey, cacheName, evictAnnotation);
        } else {
            if (evictAnnotation.delayedDoubleDelete()) {
                // 延迟双删策略
                evictCache(fullKey, cacheName, evictAnnotation);
                
                // 根据配置的延迟时间进行二次删除
                long delay = evictAnnotation.delayMillis();
                scheduler.schedule(() -> {
                    evictCache(fullKey, cacheName, evictAnnotation);
                    log.debug("延迟双删执行: key={}", fullKey);
                }, delay, TimeUnit.MILLISECONDS);
            } else {
                evictCache(fullKey, cacheName, evictAnnotation);
            }
        }

        return result;
    }

    /**
     * 获取本地缓存
     */
    private Cache<String, Object> getLocalCache(String cacheName, MultiLevelCache annotation) {
        return localCaches.computeIfAbsent(cacheName,
            name -> Caffeine.newBuilder()
                    .maximumSize(annotation.localMaxSize())
                    .expireAfterWrite(annotation.localTtl(), annotation.localTimeUnit())
                    .recordStats()
                    .build()
        );
    }

    /**
     * 同步加载（防止缓存击穿）
     */
    private Object loadWithLock(String key, MultiLevelCache annotation, ProceedingJoinPoint point)
            throws Throwable {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());

        if (lock.tryLock(annotation.syncTimeout(), TimeUnit.MILLISECONDS)) {
            try {
                // 双重检查
                if (annotation.enableRedis()) {
                    String redisValue = redisTemplate.opsForValue().get(key);
                    if (StrUtil.isNotBlank(redisValue)) {
                        return cn.hutool.json.JSONUtil.toBean(redisValue,
                                ((MethodSignature) point.getSignature()).getMethod().getReturnType());
                    }
                }
                return point.proceed();
            } finally {
                lock.unlock();
            }
        } else {
            log.warn("获取缓存锁超时: key={}", key);
            return point.proceed();
        }
    }

    /**
     * 写入缓存
     */
    private void writeCache(String key, Object value, MultiLevelCache annotation) {
        String jsonValue = cn.hutool.json.JSONUtil.toJsonStr(value);
        String cacheName = key.substring(0, key.indexOf(":"));

        // 写入本地缓存
        if (annotation.enableLocal()) {
            Cache<String, Object> localCache = getLocalCache(cacheName, annotation);
            localCache.put(key, value);
        }

        // 写入Redis
        if (annotation.enableRedis()) {
            redisTemplate.opsForValue().set(key, jsonValue,
                    annotation.redisTtl(), annotation.redisTimeUnit());
        }

        log.debug("写入多级缓存: key={}, local={}, redis={}",
                key, annotation.enableLocal(), annotation.enableRedis());
    }

    /**
     * 调度预刷新任务
     */
    private void scheduleRefreshAhead(String key, MultiLevelCache annotation, ProceedingJoinPoint point) {
        if (!annotation.enableRefreshAhead()) {
            return;
        }

        // 计算预刷新时间点（TTL的75%处）
        long refreshTime = (long) (annotation.redisTtl() * 0.75);
        
        scheduler.schedule(() -> {
            // 异步刷新缓存
            try {
                // 重新执行原方法获取最新数据
                Object newValue = point.proceed();
                if (newValue != null) {
                    writeCache(key, newValue, annotation);
                    log.debug("预刷新缓存成功: key={}", key);
                }
            } catch (Throwable e) {
                log.error("预刷新缓存失败: key={}", key, e);
            }
        }, refreshTime, annotation.redisTimeUnit());

        log.debug("已调度预刷新任务: key={}, refreshTime={}{}", 
                 key, refreshTime, annotation.redisTimeUnit());
    }

    /**
     * 清除缓存
     */
    private void evictCache(String key, String cacheName, MultiLevelCacheEvict annotation) {
        // 清除本地缓存
        if (annotation.local()) {
            Cache<String, Object> localCache = localCaches.get(cacheName);
            if (localCache != null) {
                if (annotation.allEntries()) {
                    localCache.invalidateAll();
                    log.debug("清除所有本地缓存: name={}", cacheName);
                } else {
                    localCache.invalidate(key);
                    log.debug("清除本地缓存: key={}", key);
                }
            }
        }

        // 清除Redis缓存
        if (annotation.redis()) {
            if (annotation.allEntries()) {
                String pattern = cacheName + ":*";
                var keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.debug("清除所有Redis缓存: pattern={}", pattern);
                }
            } else {
                redisTemplate.delete(key);
                log.debug("清除Redis缓存: key={}", key);
            }
        }
    }

    /**
     * 解析SpEL表达式（Key）
     */
    private String parseKey(String expression, ProceedingJoinPoint point) {
        if (StrUtil.isBlank(expression)) {
            // 默认使用方法参数拼接
            StringBuilder sb = new StringBuilder();
            Object[] args = point.getArgs();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(":");
                sb.append(args[i] != null ? args[i].toString() : "null");
            }
            return sb.toString();
        }

        return parseExpression(expression, point);
    }

    /**
     * 解析SpEL表达式（条件）
     */
    private boolean parseCondition(String expression, ProceedingJoinPoint point) {
        Object result = parseExpression(expression, point);
        return result instanceof Boolean ? (Boolean) result : false;
    }

    /**
     * 解析SpEL表达式
     */
    private String parseExpression(String expression, ProceedingJoinPoint point) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("args", point.getArgs());

        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        String[] paramNames = discoverer.getParameterNames(method);
        Object[] args = point.getArgs();

        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }

        Object result = parser.parseExpression(expression).getValue(context);
        return result != null ? result.toString() : "";
    }
}