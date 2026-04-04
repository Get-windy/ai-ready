package cn.aiedge.notification.cache;

import cn.aiedge.notification.config.NotificationProperties;
import cn.aiedge.notification.entity.NotificationTemplate;
import cn.aiedge.notification.service.impl.NotificationServiceImpl.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 通知模板缓存
 * 提供模板的内存缓存，减少数据库查询
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTemplateCache {

    private final TemplateRepository templateRepository;
    private final NotificationProperties properties;

    /**
     * 模板缓存：templateCode -> Template
     */
    private final Map<String, CacheEntry> templateCache = new ConcurrentHashMap<>();

    /**
     * 模板ID索引：id -> templateCode
     */
    private final Map<Long, String> idIndex = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        if (!properties.isTemplateCacheEnabled()) {
            log.info("模板缓存未启用");
            return;
        }

        // 初始加载所有模板
        refreshCache();

        // 定时刷新缓存
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
            this::refreshCache,
            properties.getTemplateCacheExpireSeconds(),
            properties.getTemplateCacheExpireSeconds(),
            TimeUnit.SECONDS
        );

        log.info("模板缓存初始化完成，定时刷新间隔: {}秒", properties.getTemplateCacheExpireSeconds());
    }

    /**
     * 根据编码获取模板
     */
    public NotificationTemplate getByCode(String templateCode) {
        if (!properties.isTemplateCacheEnabled()) {
            return templateRepository.findByTemplateCode(templateCode).orElse(null);
        }

        CacheEntry entry = templateCache.get(templateCode);
        if (entry != null && !entry.isExpired()) {
            return entry.template;
        }

        // 缓存未命中或已过期，从数据库加载
        Optional<NotificationTemplate> opt = templateRepository.findByTemplateCode(templateCode);
        opt.ifPresent(t -> putCache(t));
        return opt.orElse(null);
    }

    /**
     * 根据ID获取模板
     */
    public NotificationTemplate getById(Long id) {
        if (!properties.isTemplateCacheEnabled()) {
            return templateRepository.findById(id).orElse(null);
        }

        String templateCode = idIndex.get(id);
        if (templateCode != null) {
            return getByCode(templateCode);
        }
        return null;
    }

    /**
     * 获取所有启用的模板
     */
    public List<NotificationTemplate> getAllEnabled() {
        if (!properties.isTemplateCacheEnabled()) {
            return templateRepository.findAll();
        }

        return templateCache.values().stream()
            .filter(e -> !e.isExpired())
            .map(e -> e.template)
            .filter(t -> t.getStatus() == NotificationTemplate.STATUS_ENABLED)
            .toList();
    }

    /**
     * 更新缓存中的模板
     */
    public void updateTemplate(NotificationTemplate template) {
        if (properties.isTemplateCacheEnabled()) {
            putCache(template);
            log.debug("模板缓存更新: code={}", template.getTemplateCode());
        }
    }

    /**
     * 从缓存移除模板
     */
    public void removeTemplate(Long templateId) {
        if (properties.isTemplateCacheEnabled()) {
            String code = idIndex.remove(templateId);
            if (code != null) {
                templateCache.remove(code);
                log.debug("模板缓存移除: id={}, code={}", templateId, code);
            }
        }
    }

    /**
     * 清空缓存
     */
    public void clear() {
        templateCache.clear();
        idIndex.clear();
        log.info("模板缓存已清空");
    }

    /**
     * 刷新缓存
     */
    public synchronized void refreshCache() {
        try {
            List<NotificationTemplate> templates = templateRepository.findAll();
            
            // 清理旧缓存
            templateCache.clear();
            idIndex.clear();
            
            // 加载新数据
            for (NotificationTemplate t : templates) {
                putCache(t);
            }
            
            log.debug("模板缓存刷新完成，共{}个模板", templates.size());
        } catch (Exception e) {
            log.error("刷新模板缓存失败", e);
        }
    }

    private void putCache(NotificationTemplate template) {
        CacheEntry entry = new CacheEntry(template, System.currentTimeMillis());
        templateCache.put(template.getTemplateCode(), entry);
        idIndex.put(template.getId(), template.getTemplateCode());
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        int total = templateCache.size();
        int enabled = (int) templateCache.values().stream()
            .filter(e -> e.template.getStatus() == NotificationTemplate.STATUS_ENABLED)
            .count();
        int expired = (int) templateCache.values().stream()
            .filter(CacheEntry::isExpired)
            .count();
        
        return new CacheStats(total, enabled, expired);
    }

    private class CacheEntry {
        final NotificationTemplate template;
        final long createdAt;
        final long expireTimeMs;

        CacheEntry(NotificationTemplate template, long now) {
            this.template = template;
            this.createdAt = now;
            this.expireTimeMs = now + properties.getTemplateCacheExpireSeconds() * 1000;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTimeMs;
        }
    }

    public record CacheStats(int total, int enabled, int expired) {}
}
