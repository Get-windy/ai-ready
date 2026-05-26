package cn.aiedge.export.service;

import java.io.InputStream;
import java.util.Map;

public interface BatchImportService {

    String createImportTask(String dataType, String fileName);

    ImportProgress getProgress(String taskId);

    boolean cancelTask(String taskId);

    ImportTemplate getTemplate(String dataType);

    void executeImport(String taskId, String dataType, InputStream inputStream, Map<String, Object> options);

    byte[] downloadTemplate(String dataType);

    record ImportProgress(
        String taskId,
        String status,
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

    record ImportTemplate(
        String dataType,
        String templateName,
        Map<String, String> headers,
        Map<String, String> fieldTypes,
        Map<String, String> validators,
        String sampleFile
    ) {}
}
