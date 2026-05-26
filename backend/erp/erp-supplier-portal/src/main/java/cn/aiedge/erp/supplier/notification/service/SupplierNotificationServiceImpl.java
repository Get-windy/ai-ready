package cn.aiedge.erp.supplier.notification.service;

import cn.aiedge.erp.supplier.entity.Supplier;
import cn.aiedge.erp.supplier.notification.config.SupplierNotificationProperties;
import cn.aiedge.erp.supplier.notification.entity.SupplierNotificationConfig;
import cn.aiedge.erp.supplier.notification.entity.SupplierNotificationRecord;
import cn.aiedge.erp.supplier.notification.event.SupplierNotificationEvent;
import cn.aiedge.erp.supplier.notification.model.SupplierNotificationRequest;
import cn.aiedge.erp.supplier.notification.model.NotificationResult;
import cn.aiedge.erp.supplier.notification.model.BatchNotificationResult;
import cn.aiedge.erp.supplier.notification.model.NotificationStatistics;
import cn.aiedge.erp.supplier.notification.model.DateRange;
import cn.aiedge.erp.supplier.notification.repository.SupplierNotificationConfigRepository;
import cn.aiedge.erp.supplier.notification.repository.SupplierNotificationRecordRepository;
import cn.aiedge.erp.supplier.service.SupplierService;
import cn.aiedge.notification.channel.NotificationChannel;
import cn.aiedge.notification.entity.NotificationRecord;
import cn.aiedge.notification.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 供应商通知服务实现
 * 整合现有通知系统，提供供应商专属功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierNotificationServiceImpl implements SupplierNotificationService {

    private final SupplierNotificationConfigRepository configRepository;
    private final SupplierNotificationRecordRepository recordRepository;
    private final NotificationService notificationService;
    private final SupplierService supplierService;
    private final SupplierNotificationProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    
    // 缓存供应商配置，减少数据库访问
    private final Map<Long, SupplierNotificationConfig> configCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public NotificationResult sendNotification(SupplierNotificationRequest request) {
        try {
            log.info("开始发送供应商通知: supplierId={}, templateCode={}", 
                    request.getSupplierId(), request.getTemplateCode());
            
            // 1. 获取供应商信息
            Supplier supplier = supplierService.getById(request.getSupplierId());
            if (supplier == null) {
                return NotificationResult.failed("供应商不存在: " + request.getSupplierId());
            }
            
            // 2. 获取供应商通知配置
            SupplierNotificationConfig config = getConfig(supplier.getId());
            
            // 3. 构建通知变量
            Map<String, Object> variables = buildVariables(request, supplier);
            
            // 4. 发送通知
            NotificationRecord record = notificationService.send(
                request.getTemplateCode(),
                supplier.getContactPersonId(),
                "SUPPLIER",
                supplier.getEmail(),
                variables
            );
            boolean success = record != null;
            
            // 5. 记录通知历史
            if (success) {
                saveNotificationRecord(request, supplier, config, true);
                eventPublisher.publishEvent(new SupplierNotificationEvent(
                    request.getSupplierId(), 
                    request.getTemplateCode(), 
                    "SUCCESS"
                ));
                
                log.info("供应商通知发送成功: supplierId={}, templateCode={}", 
                        request.getSupplierId(), request.getTemplateCode());
                return NotificationResult.success();
            } else {
                saveNotificationRecord(request, supplier, config, false);
                log.warn("供应商通知发送失败: supplierId={}, templateCode={}", 
                        request.getSupplierId(), request.getTemplateCode());
                return NotificationResult.failed("通知发送失败");
            }
        } catch (Exception e) {
            log.error("发送供应商通知异常", e);
            return NotificationResult.failed("系统异常: " + e.getMessage());
        }
    }

    @Override
    @Async
    @Transactional
    public CompletableFuture<BatchNotificationResult> sendBatchNotifications(
            List<SupplierNotificationRequest> requests) {
        BatchNotificationResult result = new BatchNotificationResult();
        
        log.info("开始批量发送供应商通知，数量: {}", requests.size());
        
        List<CompletableFuture<NotificationResult>> futures = requests.stream()
            .map(request -> CompletableFuture.supplyAsync(() -> sendNotification(request)))
            .collect(Collectors.toList());
        
        // 等待所有完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<NotificationResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                
                long successCount = results.stream().filter(NotificationResult::isSuccess).count();
                long failedCount = results.size() - successCount;
                
                result.setSuccessCount((int) successCount);
                result.setFailedCount((int) failedCount);
                result.setResults(results);
                
                log.info("批量发送完成: 成功={}, 失败={}", successCount, failedCount);
                return result;
            });
        
        return CompletableFuture.completedFuture(result);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierNotificationConfig getConfig(Long supplierId) {
        // 先从缓存获取
        SupplierNotificationConfig cached = configCache.get(supplierId);
        if (cached != null) {
            return cached;
        }
        
        // 从数据库查询
        SupplierNotificationConfig config = configRepository.findBySupplierId(supplierId)
                .orElse(null);
        if (config == null) {
            // 创建默认配置
            config = createDefaultConfig(supplierId);
            configRepository.save(config);
        }
        
        // 放入缓存
        configCache.put(supplierId, config);
        return config;
    }

    @Override
    @Transactional
    public void updateConfig(Long supplierId, SupplierNotificationConfig config) {
        config.setSupplierId(supplierId);
        config.setUpdatedAt(LocalDateTime.now());
        
        SupplierNotificationConfig saved = configRepository.save(config);
        
        // 更新缓存
        configCache.put(supplierId, saved);
        
        log.info("更新供应商通知配置: supplierId={}", supplierId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierNotificationRecord> getHistory(Long supplierId, DateRange range) {
        LocalDateTime start = range.getStart();
        LocalDateTime end = range.getEnd();
        
        return recordRepository.findBySupplierIdAndCreatedAtBetween(
            supplierId, start, end
        );
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationStatistics getStatistics(Long supplierId, DateRange range) {
        LocalDateTime start = range.getStart();
        LocalDateTime end = range.getEnd();
        
        // 查询统计信息
        long totalCount = recordRepository.countBySupplierIdAndCreatedAtBetween(supplierId, start, end);
        long successCount = recordRepository.countBySupplierIdAndStatusAndCreatedAtBetween(
            supplierId, "SUCCESS", start, end);
        long readCount = recordRepository.countBySupplierIdAndReadTimeIsNotNullAndCreatedAtBetween(
            supplierId, start, end);
        
        // 按模板类型统计
        Map<String, Long> templateStats = recordRepository.groupByTemplateCode(
            supplierId, start, end);
        
        // 按渠道统计
        Map<String, Long> channelStats = recordRepository.groupByChannel(
            supplierId, start, end);
        
        return NotificationStatistics.builder()
            .totalCount(totalCount)
            .successCount(successCount)
            .readCount(readCount)
            .successRate(totalCount > 0 ? (double) successCount / totalCount * 100 : 0)
            .readRate(successCount > 0 ? (double) readCount / successCount * 100 : 0)
            .templateStatistics(templateStats)
            .channelStatistics(channelStats)
            .build();
    }

    // ==================== 私有方法 ====================

    /**
     * 创建默认通知配置
     */
    private SupplierNotificationConfig createDefaultConfig(Long supplierId) {
        SupplierNotificationConfig config = new SupplierNotificationConfig();
        config.setSupplierId(supplierId);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        
        // 默认渠道配置
        Map<String, Object> configMap = new HashMap<>();
        
        // 采购订单通知配置
        configMap.put("purchaseOrder", Map.of(
            "channels", Arrays.asList("in-app", "email", "sms"),
            "priority", "high",
            "quietHours", Map.of("start", "22:00", "end", "08:00")
        ));
        
        // 付款通知配置
        configMap.put("payment", Map.of(
            "channels", Arrays.asList("in-app", "email", "sms", "voice"),
            "priority", "high",
            "amountThreshold", 10000
        ));
        
        // 质量通知配置
        configMap.put("quality", Map.of(
            "channels", Arrays.asList("in-app", "email"),
            "priority", "medium"
        ));
        
        // 绩效通知配置
        configMap.put("performance", Map.of(
            "channels", Arrays.asList("email"),
            "priority", "low",
            "frequency", "monthly"
        ));
        
        // 将 Map 转换为 JSON 字符串
        try {
            config.setConfigJson(new ObjectMapper().writeValueAsString(configMap));
        } catch (JsonProcessingException e) {
            log.error("序列化配置JSON失败", e);
            config.setConfigJson("{}");
        }
        return config;
    }

    /**
     * 构建通知变量
     */
    private Map<String, Object> buildVariables(
            SupplierNotificationRequest request,
            Supplier supplier) {
        
        // 合并变量
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(getSupplierVariables(supplier));
        if (request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }
        
        // 添加系统变量
        variables.put("currentDate", LocalDateTime.now().toLocalDate());
        variables.put("currentTime", LocalDateTime.now());
        variables.put("systemName", "AI-Ready ERP");
        
        return variables;
    }

    /**
     * 确定使用的通知渠道
     */
    private String determineChannels(SupplierNotificationRequest request, 
                                     SupplierNotificationConfig config) {
        
        // 从配置中获取业务类型对应的渠道
        String businessType = request.getTemplateCode().split("_")[0].toLowerCase();
        
        // 解析 JSON 配置
        Map<String, Object> configMap = parseConfigJson(config.getConfigJson());
        @SuppressWarnings("unchecked")
        Map<String, Object> typeConfig = (Map<String, Object>) configMap.get(businessType);
        
        if (typeConfig != null) {
            @SuppressWarnings("unchecked")
            List<String> channels = (List<String>) typeConfig.get("channels");
            if (channels != null && !channels.isEmpty()) {
                return String.join(",", channels);
            }
        }
        
        // 默认渠道
        return properties.getDefaultChannels();
    }

    /**
     * 确定通知优先级
     */
    private String determinePriority(SupplierNotificationRequest request,
                                     SupplierNotificationConfig config) {
        
        // 根据模板类型确定优先级
        String templateCode = request.getTemplateCode();
        
        // 紧急通知类型
        if (templateCode.contains("URGENT") || 
            templateCode.contains("PAYMENT") ||
            templateCode.contains("CRITICAL")) {
            return "urgent";
        }
        
        // 业务通知类型
        if (templateCode.contains("ORDER") || 
            templateCode.contains("QUALITY") ||
            templateCode.contains("DELIVERY")) {
            return "high";
        }
        
        // 其他通知
        return "normal";
    }

    /**
     * 获取供应商相关变量
     */
    private Map<String, Object> getSupplierVariables(Supplier supplier) {
        Map<String, Object> vars = new HashMap<>();
        
        vars.put("supplierName", supplier.getName());
        vars.put("supplierCode", supplier.getCode());
        vars.put("supplierContact", supplier.getContactPerson());
        vars.put("supplierEmail", supplier.getEmail());
        vars.put("supplierPhone", supplier.getPhone());
        vars.put("supplierAddress", supplier.getAddress());
        vars.put("supplierCategory", supplier.getCategory());
        
        return vars;
    }

    /**
     * 保存通知记录
     */
    private void saveNotificationRecord(SupplierNotificationRequest request,
                                        Supplier supplier,
                                        SupplierNotificationConfig config,
                                        boolean success) {
        
        SupplierNotificationRecord record = new SupplierNotificationRecord();
        record.setSupplierId(supplier.getId());
        record.setSupplierName(supplier.getName());
        record.setTemplateCode(request.getTemplateCode());
        
        // 设置标题和内容（简化处理）
        record.setTitle("供应商通知: " + request.getTemplateCode());
        record.setContent(buildContent(request.getVariables()));
        
        // 设置渠道
        String channels = determineChannels(request, config);
        record.setChannels(channels);
        
        // 设置状态
        record.setStatus(success ? "SUCCESS" : "FAILED");
        record.setSendTime(LocalDateTime.now());
        
        // 设置业务上下文
        if (request.getBusinessContext() != null) {
            record.setBusinessType(request.getBusinessContext().getType());
            record.setBusinessId(request.getBusinessContext().getId());
        }
        
        record.setCreatedAt(LocalDateTime.now());
        
        recordRepository.save(record);
    }

    /**
     * 构建通知内容
     */
    private String buildContent(Map<String, Object> variables) {
        StringBuilder content = new StringBuilder();
        
        content.append("尊敬的供应商，");
        content.append("\n\n");
        
        variables.forEach((key, value) -> {
            if (!key.startsWith("_") && value != null) {
                content.append(key).append(": ").append(value).append("\n");
            }
        });
        
        content.append("\n");
        content.append("感谢您使用AI-Ready ERP系统");
        
        return content.toString();
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 清除缓存
     */
    public void clearCache(Long supplierId) {
        if (supplierId == null) {
            configCache.clear();
        } else {
            configCache.remove(supplierId);
        }
    }
    
    /**
     * 预加载供应商配置到缓存
     */
    public void preloadConfigs(List<Long> supplierIds) {
        supplierIds.forEach(this::getConfig);
    }
    
    /**
     * 检查供应商是否有配置
     */
    public boolean hasConfig(Long supplierId) {
        return configRepository.existsBySupplierId(supplierId);
    }
    
    /**
     * 获取供应商的紧急联系人
     */
    public List<String> getEmergencyContacts(Long supplierId) {
        Supplier supplier = supplierService.getById(supplierId);
        if (supplier == null) {
            return Collections.emptyList();
        }
        
        List<String> contacts = new ArrayList<>();
        
        if (StrUtil.isNotBlank(supplier.getEmergencyPhone())) {
            contacts.add(supplier.getEmergencyPhone());
        }
        
        if (StrUtil.isNotBlank(supplier.getPhone())) {
            contacts.add(supplier.getPhone());
        }
        
        return contacts;
    }
    
    /**
     * 解析配置JSON字符串
     */
    private Map<String, Object> parseConfigJson(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return new HashMap<>();
        }
        
        try {
            return new ObjectMapper().readValue(configJson, 
                new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("解析配置JSON失败", e);
            return new HashMap<>();
        }
    }
}