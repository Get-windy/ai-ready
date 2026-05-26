package cn.aiedge.export.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 数据导入导出服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface DataExportService {

    // ==================== 导出功能 ====================

    /**
     * 导出为Excel
     *
     * @param data     数据列表
     * @param headers  表头定义（字段名 -> 显示名称）
     * @param out      输出流
     * @param <T>      数据类型
     */
    <T> void exportExcel(List<T> data, Map<String, String> headers, OutputStream out);

    /**
     * 导出为CSV
     *
     * @param data     数据列表
     * @param headers  表头定义
     * @param out      输出流
     * @param <T>      数据类型
     */
    <T> void exportCsv(List<T> data, Map<String, String> headers, OutputStream out);

    /**
     * 分批导出大数据量Excel
     *
     * @param dataProvider 数据提供者（分页查询）
     * @param headers      表头定义
     * @param out          输出流
     * @param batchSize    每批数量
     * @param <T>          数据类型
     */
    <T> void exportExcelBatch(DataProvider<T> dataProvider, Map<String, String> headers,
                              OutputStream out, int batchSize);

    // ==================== 导入功能 ====================

    /**
     * 从Excel导入
     *
     * @param in        输入流
     * @param headers   表头定义
     * @param rowClass  行数据类型
     * @param <T>       数据类型
     * @return 数据列表
     */
    <T> List<T> importExcel(InputStream in, Map<String, String> headers, Class<T> rowClass);

    /**
     * 从CSV导入
     *
     * @param in      输入流
     * @param headers 表头定义
     * @param rowClass 行数据类型
     * @param <T>     数据类型
     * @return 数据列表
     */
    <T> List<T> importCsv(InputStream in, Map<String, String> headers, Class<T> rowClass);

    /**
     * 分批导入大数据量Excel
     *
     * @param in        输入流
     * @param headers   表头定义
     * @param rowClass  行数据类型
     * @param batchSize 每批数量
     * @param processor 批量处理器
     * @param <T>       数据类型
     * @return 导入结果
     */
    <T> ImportResult importExcelBatch(InputStream in, Map<String, String> headers,
                                       Class<T> rowClass, int batchSize,
                                       Consumer<List<T>> processor);

    /**
     * 从Excel导入并校验
     *
     * @param in        输入流
     * @param headers   表头定义
     * @param rowClass  行数据类型
     * @param validator 数据校验器
     * @param <T>       数据类型
     * @return 导入结果（包含成功和失败数据）
     */
    <T> ImportResult importExcelWithValidation(InputStream in, Map<String, String> headers,
                                                Class<T> rowClass, DataValidator<T> validator);

    // ==================== 辅助类型 ====================

    /**
     * 数据提供者接口（用于分批导出）
     */
    @FunctionalInterface
    interface DataProvider<T> {
        List<T> fetchData(int page, int size);
    }

    /**
     * 数据校验器接口
     */
    @FunctionalInterface
    interface DataValidator<T> {
        ValidationResult validate(T data, int rowIndex);
    }

    /**
     * 校验结果
     */
    record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * 导入结果
     */
    record ImportResult(int totalCount, int successCount, int failureCount,
                        List<ImportError> errors) {
        public static ImportResult success(int count) {
            return new ImportResult(count, count, 0, List.of());
        }
    }

    /**
     * 导入错误信息
     */
    record ImportError(int rowIndex, String fieldName, String errorMessage) {}
}
