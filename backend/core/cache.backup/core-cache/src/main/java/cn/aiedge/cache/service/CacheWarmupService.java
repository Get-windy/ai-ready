package cn.aiedge.cache.service;

import cn.aiedge.user.service.UserService;
import cn.aiedge.permission.service.PermissionService;
import cn.aiedge.system.service.SysConfigService;
import cn.aiedge.dict.service.DictItemService;
import cn.aiedge.dict.service.DictTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 缓存预热服务
 * 系统启动时自动预热热点数据
 * 
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class CacheWarmupService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private SysConfigService configService;
    
    @Autowired
    private DictItemService dictItemService;
    
    @Autowired
    private DictTypeService dictTypeService;
    
    @Resource(name = "cacheExecutor")
    private Executor cacheExecutor;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 系统启动完成后预热缓存
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void performCacheWarmup() {
        log.info("开始执行缓存预热...");
        
        try {
            // 并行预热不同类型的数据
            CompletableFuture<Void> userWarmup = CompletableFuture.runAsync(this::warmupUserPermissions, cacheExecutor);
            CompletableFuture<Void> configWarmup = CompletableFuture.runAsync(this::warmupSystemConfigs, cacheExecutor);
            CompletableFuture<Void> dictWarmup = CompletableFuture.runAsync(this::warmupDictData, cacheExecutor);
            
            // 等待所有预热任务完成
            CompletableFuture.allOf(userWarmup, configWarmup, dictWarmup).join();
            
            log.info("缓存预热完成");
        } catch (Exception e) {
            log.error("缓存预热失败", e);
        }
    }
    
    /**
     * 预热用户权限数据
     */
    private void warmupUserPermissions() {
        log.info("开始预热用户权限数据...");
        
        try {
            // 获取活跃用户（这里可以根据实际业务调整获取用户的策略）
            List<Long> userIds = userService.getActiveUserIds();
            log.info("准备预热 {} 个用户的权限数据", userIds.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Long userId : userIds) {
                try {
                    // 调用权限服务获取用户权限（会自动缓存）
                    permissionService.getUserPermissions(userId);
                    successCount++;
                    
                    if (successCount % 50 == 0) {
                        log.debug("已预热 {} 个用户权限", successCount);
                    }
                } catch (Exception e) {
                    log.warn("预热用户权限失败: userId={}", userId, e);
                    failureCount++;
                }
            }
            
            log.info("用户权限预热完成，成功: {}, 失败: {}", successCount, failureCount);
        } catch (Exception e) {
            log.error("预热用户权限数据时发生异常", e);
        }
    }
    
    /**
     * 预热系统配置数据
     */
    private void warmupSystemConfigs() {
        log.info("开始预热系统配置数据...");
        
        try {
            // 获取所有系统配置项并预热
            List<String> configKeys = configService.getAllConfigKeys();
            log.info("准备预热 {} 个系统配置", configKeys.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (String configKey : configKeys) {
                try {
                    configService.getConfigValue(configKey);
                    successCount++;
                } catch (Exception e) {
                    log.warn("预热系统配置失败: key={}", configKey, e);
                    failureCount++;
                }
            }
            
            log.info("系统配置预热完成，成功: {}, 失败: {}", successCount, failureCount);
        } catch (Exception e) {
            log.error("预热系统配置数据时发生异常", e);
        }
    }
    
    /**
     * 预热字典数据
     */
    private void warmupDictData() {
        log.info("开始预热字典数据...");
        
        try {
            List<String> dictTypes = dictTypeService.getAllDictTypes();
            log.info("准备预热 {} 个字典类型", dictTypes.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (String dictType : dictTypes) {
                try {
                    dictItemService.getDictItemList(dictType);
                    successCount++;
                } catch (Exception e) {
                    log.warn("预热字典数据失败: type={}", dictType, e);
                    failureCount++;
                }
            }
            
            log.info("字典数据预热完成，成功: {}, 失败: {}", successCount, failureCount);
        } catch (Exception e) {
            log.error("预热字典数据时发生异常", e);
        }
    }
}