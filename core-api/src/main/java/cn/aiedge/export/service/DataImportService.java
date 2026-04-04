package cn.aiedge.export.service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 数据导入服务接口
 * 
 * 提供Excel和CSV文件导入功能
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface DataImportService {

    /**
     * 导入Excel文件
     *
     * @param inputStream 输入流
     * @param dataType    数据类型
     * @param tenantId    租户ID
     * @return 导入结果
     */
    ImportResult importExcel(InputStream inputStream, String dataType, Long tenantId);

    /**
     * 导入CSV文件
     *
     * @param inputStream 输入流
     * @param dataType    数据类型
     * @param tenantId    租户ID
     * @return 导入结果
     */
    ImportResult importCsv(InputStream inputStream, String dataType, Long tenantId);

    /**
     * 预览导入数据
     *
     * @param inputStream 输入流
     * @param dataType    数据类型
     * @param maxRows     最大行数
     * @return 预览数据
     */
    PreviewResult preview(InputStream inputStream, String dataType, int maxRows);

    /**
     * 校验导入数据
     *
     * @param dataList 数据列表
     * @param dataType 数据类型
     * @return 校验结果
     */
    ValidateResult validate(List<Map<String, Object>> dataList, String dataType);

    /**
     * 获取导入模板字段定义
     *
     * @param dataType 数据类型
     * @return 字段定义列表
     */
    List<FieldDefinition> getFieldDefinitions(String dataType);

    /**
     * 导入结果
     */
    record ImportResult(
        boolean success,
        int totalCount,
        int successCount,
        int failureCount,
        List<ImportError> errors,
        String message
    ) {
        public static ImportResult success(int total, int success) {
            return new ImportResult(true, total, success, 0, List.of(), "导入成功");
        }

        public static ImportResult partial(int total, int success, int failure, List<ImportError> errors) {
            return new ImportResult(true, total, success, failure, errors, 
                String.format("导入完成，成功%d条，失败%d条", success, failure));
        }

        public static ImportResult failure(String message) {
            return new ImportResult(false, 0, 0, 0, List.of(), message);
        }
    }

    /**
     * 导入错误
     */
    record ImportError(
        int rowIndex,
        String field,
        String value,
        String errorMessage
    ) {}

    /**
     * 预览结果
     */
    record PreviewResult(
        List<String> headers,
        List<Map<String, Object>> rows,
        int totalRows,
        int previewRows
    ) {}

    /**
     * 校验结果
     */
    record ValidateResult(
        boolean valid,
        int totalRows,
        int validRows,
        int invalidRows,
        List<ValidateError> errors
    ) {
        public static ValidateResult valid(int total) {
            return new ValidateResult(true, total, total, 0, List.of());
        }

        public static ValidateResult invalid(int total, int valid, int invalid, List<ValidateError> errors) {
            return new ValidateResult(false, total, valid, invalid, errors);
        }
    }

    /**
     * 校验错误
     */
    record ValidateError(
        int rowIndex,
        String field,
        String value,
        String rule,
        String message
    ) {}

    /**
     * 字段定义
     */
    record FieldDefinition(
        String field,
        String label,
        String type,
        boolean required,
        int maxLength,
        String pattern,
        String description,
        List<FieldValue> options
    ) {}

    /**
     * 字段可选值
     */
    record FieldValue(
        String value,
        String label
    ) {}
}
