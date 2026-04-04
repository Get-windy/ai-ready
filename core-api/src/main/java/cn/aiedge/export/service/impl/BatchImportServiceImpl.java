package cn.aiedge.export.service.impl;

import cn.aiedge.export.service.BatchImportService;
import cn.aiedge.export.service.DataExportService;
import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批量导入服务实现
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchImportServiceImpl implements BatchImportService {

    private final DataExportService dataExportService;
    private final StringRedisTemplate redisTemplate;
    
    // 任务缓存
    private static final Map<String, ImportProgress> TASK_CACHE = new ConcurrentHashMap<>();
    private static final String TASK_KEY_PREFIX = "import:task:";
    
    // 数据类型配置
    private static final Map<String, Map<String, String>> DATA_TYPE_CONFIG = Map.of(
        "user", Map.of(
            "username", "用户名",
            "email", "邮箱",
            "phone", "手机号",
            "realName", "真实姓名",
            "deptId", "部门ID",
            "status", "状态"
        ),
        "customer", Map.of(
            "customerName", "客户名称",
            "contact", "联系人",
            "phone", "联系电话",
            "email", "邮箱",
            "address", "地址",
            "remark", "备注"
        ),
        "product", Map.of(
            "productCode", "产品编码",
            "productName", "产品名称",
            "category", "分类",
            "price", "价格",
            "stock", "库存",
            "status", "状态"
        )
    );

    @Override
    public String createImportTask(String dataType, String fileName) {
        String taskId = IdUtil.fastSimpleUUID();
        
        ImportProgress progress = new ImportProgress(
            taskId,
            "pending",
            0, 0, 0, 0,
            "任务已创建，等待处理",
            null,
            System.currentTimeMillis(),
            0
        );
        
        TASK_CACHE.put(taskId, progress);
        
        // 保存到Redis
        saveProgress(progress);
        
        log.info("创建导入任务: taskId={}, dataType={}, fileName={}", taskId, dataType, fileName);
        return taskId;
    }

    /**
     * 异步执行导入任务
     */
    @Async
    public void executeImport(String taskId, String dataType, InputStream inputStream, 
                              ImportHandler handler) {
        ImportProgress progress = TASK_CACHE.get(taskId);
        if (progress == null) {
            log.warn("任务不存在: {}", taskId);
            return;
        }
        
        try {
            // 更新状态为处理中
            updateProgress(taskId, "processing", 0, 0, 0, "正在读取文件...");
            
            Map<String, String> headers = DATA_TYPE_CONFIG.get(dataType);
            if (headers == null) {
                throw new RuntimeException("不支持的数据类型: " + dataType);
            }
            
            // 使用带校验的导入
            DataExportService.ImportResult result = dataExportService.importExcelWithValidation(
                inputStream,
                headers,
                Map.class,
                (data, rowIndex) -> {
                    // 自定义校验逻辑
                    return validateRow(data, rowIndex, headers);
                }
            );
            
            // 处理成功数据
            List<Object> successData = new ArrayList<>();
            List<Map<String, Object>> allData = (List<Map<String, Object>>) 
                dataExportService.importExcel(inputStream, headers, Map.class);
            
            for (int i = 0; i < allData.size(); i++) {
                if (handler != null) {
                    try {
                        handler.processRow(allData.get(i), i + 2);
                        updateProgress(taskId, "processing", allData.size(), i + 1, 
                            result.failureCount(), "正在处理第 " + (i + 1) + " 行");
                    } catch (Exception e) {
                        log.warn("处理第{}行失败: {}", i + 2, e.getMessage());
                    }
                }
            }
            
            // 更新完成状态
            if (result.failureCount() > 0) {
                // 生成错误文件
                String errorFile = generateErrorFile(result.errors());
                updateProgress(taskId, "completed", result.totalCount(), result.successCount(),
                    result.failureCount(), "导入完成，部分数据失败", errorFile);
            } else {
                updateProgress(taskId, "completed", result.totalCount(), result.successCount(),
                    0, "导入完成");
            }
            
        } catch (Exception e) {
            log.error("导入任务执行失败: taskId={}", taskId, e);
            updateProgress(taskId, "failed", 0, 0, 0, "导入失败: " + e.getMessage());
        }
    }

    @Override
    public ImportProgress getProgress(String taskId) {
        ImportProgress progress = TASK_CACHE.get(taskId);
        if (progress == null) {
            // 尝试从Redis获取
            progress = loadProgress(taskId);
        }
        return progress;
    }

    @Override
    public boolean cancelTask(String taskId) {
        ImportProgress progress = TASK_CACHE.get(taskId);
        if (progress != null && "pending".equals(progress.status())) {
            updateProgress(taskId, "cancelled", progress.totalCount(), 
                progress.successCount(), progress.failureCount(), "任务已取消");
            return true;
        }
        return false;
    }

    @Override
    public ImportTemplate getTemplate(String dataType) {
        Map<String, String> headers = DATA_TYPE_CONFIG.get(dataType);
        if (headers == null) {
            throw new RuntimeException("不支持的数据类型: " + dataType);
        }
        
        return new ImportTemplate(
            dataType,
            dataType + "_import_template",
            headers,
            Map.of(), // 字段类型
            Map.of(), // 校验规则
            null      // 示例文件URL
        );
    }

    /**
     * 下载模板
     */
    public byte[] downloadTemplate(String dataType) {
        Map<String, String> headers = DATA_TYPE_CONFIG.get(dataType);
        if (headers == null) {
            throw new RuntimeException("不支持的数据类型: " + dataType);
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        dataExportService.exportExcel(Collections.emptyList(), headers, out);
        return out.toByteArray();
    }

    // ==================== 辅助方法 ====================

    private void updateProgress(String taskId, String status, int total, int success, 
                                int failure, String message) {
        updateProgress(taskId, status, total, success, failure, message, null);
    }

    private void updateProgress(String taskId, String status, int total, int success, 
                                int failure, String message, String errorFile) {
        ImportProgress current = TASK_CACHE.get(taskId);
        if (current == null) {
            return;
        }
        
        ImportProgress updated = new ImportProgress(
            taskId,
            status,
            total,
            success,
            failure,
            Math.max(success, current.currentRow()),
            message,
            errorFile,
            current.startTime(),
            "completed".equals(status) ? System.currentTimeMillis() : 0
        );
        
        TASK_CACHE.put(taskId, updated);
        saveProgress(updated);
    }

    private void saveProgress(ImportProgress progress) {
        try {
            String key = TASK_KEY_PREFIX + progress.taskId();
            // 简化存储，实际应使用JSON序列化
            redisTemplate.opsForHash().put(key, "status", progress.status());
            redisTemplate.opsForHash().put(key, "total", String.valueOf(progress.totalCount()));
            redisTemplate.opsForHash().put(key, "success", String.valueOf(progress.successCount()));
            redisTemplate.opsForHash().put(key, "failure", String.valueOf(progress.failureCount()));
            redisTemplate.opsForHash().put(key, "message", progress.message());
        } catch (Exception e) {
            log.warn("保存进度失败: {}", e.getMessage());
        }
    }

    private ImportProgress loadProgress(String taskId) {
        try {
            String key = TASK_KEY_PREFIX + taskId;
            Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
            if (map.isEmpty()) {
                return null;
            }
            return new ImportProgress(
                taskId,
                (String) map.get("status"),
                Integer.parseInt((String) map.getOrDefault("total", "0")),
                Integer.parseInt((String) map.getOrDefault("success", "0")),
                Integer.parseInt((String) map.getOrDefault("failure", "0")),
                Integer.parseInt((String) map.getOrDefault("total", "0")),
                (String) map.get("message"),
                null,
                0, 0
            );
        } catch (Exception e) {
            return null;
        }
    }

    private DataExportService.ValidationResult validateRow(Object data, int rowIndex, 
                                                           Map<String, String> headers) {
        // 基本校验逻辑
        if (data == null) {
            return DataExportService.ValidationResult.failure("数据为空");
        }
        return DataExportService.ValidationResult.success();
    }

    private String generateErrorFile(List<DataExportService.ImportError> errors) {
        if (errors == null || errors.isEmpty()) {
            return null;
        }
        // 实际应生成错误文件并返回URL
        return "error_" + System.currentTimeMillis() + ".xlsx";
    }

    /**
     * 导入处理器接口
     */
    @FunctionalInterface
    public interface ImportHandler {
        void processRow(Map<String, Object> rowData, int rowIndex) throws Exception;
    }
}
