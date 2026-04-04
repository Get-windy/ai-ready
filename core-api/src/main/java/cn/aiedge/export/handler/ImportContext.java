package cn.aiedge.export.handler;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入上下文
 * 跟踪导入过程中的状态和错误
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public class ImportContext<T> {

    private final String taskId;
    private final String filename;
    private final int totalRows;
    private final Class<T> rowClass;

    private int processedRows = 0;
    private int successCount = 0;
    private int failureCount = 0;
    private final List<RowError> errors = new ArrayList<>();
    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;

    /**
     * 记录成功
     */
    public void recordSuccess() {
        processedRows++;
        successCount++;
    }

    /**
     * 记录失败
     */
    public void recordFailure(int rowIndex, String field, String message) {
        processedRows++;
        failureCount++;
        errors.add(new RowError(rowIndex, field, message));
    }

    /**
     * 完成导入
     */
    public void finish() {
        this.endTime = LocalDateTime.now();
    }

    /**
     * 是否完成
     */
    public boolean isFinished() {
        return endTime != null;
    }

    /**
     * 获取进度百分比
     */
    public double getProgress() {
        if (totalRows == 0) return 0;
        return (double) processedRows / totalRows * 100;
    }

    /**
     * 获取错误摘要
     */
    public String getErrorSummary() {
        if (errors.isEmpty()) {
            return "无错误";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("共").append(errors.size()).append("条错误：\n");

        // 只显示前10条错误
        int count = Math.min(10, errors.size());
        for (int i = 0; i < count; i++) {
            RowError error = errors.get(i);
            sb.append("第").append(error.rowIndex).append("行: ")
              .append(error.message).append("\n");
        }

        if (errors.size() > 10) {
            sb.append("... 还有").append(errors.size() - 10).append("条错误");
        }

        return sb.toString();
    }

    /**
     * 行错误信息
     */
    @Getter
    @RequiredArgsConstructor
    public static class RowError {
        private final int rowIndex;
        private final String field;
        private final String message;
    }
}
