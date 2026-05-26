package cn.aiedge.export.service;

import java.util.Map;

/**
 * 批量导入服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface BatchImportService {

    /**
     * 创建导入任务
     *
     * @param dataType 数据类型
     * @param fileName 文件名
     * @return 任务ID
     */
    String createImportTask(String dataType, String fileName);

    /**
     * 获取导入进度
     *
     * @param taskId 任务ID
     * @return 进度信息
     */
    ImportProgress getProgress(String taskId);

    /**
     * 取消导入任务
     *
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(String taskId);

    /**
     * 获取导入模板
     *
     * @param dataType 数据类型
     * @return 模板配置
     */
    ImportTemplate getTemplate(String dataType);

    /**
     * 导入进度信息
     */
    record ImportProgress(
        String taskId,
        String status,          // pending, processing, completed, failed, cancelled
        int totalCount,
        int successCount,
        int failureCount,
        int currentRow,
        String message,
        String errorFile,
        long startTime,
        long endTime
    ) {
        public int getPercentage() {
            if (totalCount == 0) return 0;
            return (int) ((double) currentRow / totalCount * 100);
        }
    }

    /**
     * 导入模板配置
     */
    record ImportTemplate(
        String dataType,
        String templateName,
        Map<String, String> headers,
        Map<String, String> fieldTypes,
        Map<String, String> validators,
        String sampleFile
    ) {}
}
