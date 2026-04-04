package cn.aiedge.export.handler;

import lombok.Getter;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 导出配置构建器
 * 简化导出配置的定义
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public class ExportConfig<T> {

    private final Class<T> dataClass;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final List<String> excludeFields = new ArrayList<>();
    private String sheetName = "Sheet1";
    private boolean autoWidth = true;

    private ExportConfig(Class<T> dataClass) {
        this.dataClass = dataClass;
    }

    /**
     * 创建导出配置
     */
    public static <T> ExportConfig<T> create(Class<T> dataClass) {
        return new ExportConfig<>(dataClass);
    }

    /**
     * 添加字段映射
     */
    public ExportConfig<T> field(String fieldName, String displayName) {
        this.headers.put(fieldName, displayName);
        return this;
    }

    /**
     * 批量添加字段映射
     */
    public ExportConfig<T> fields(Map<String, String> fields) {
        this.headers.putAll(fields);
        return this;
    }

    /**
     * 排除字段
     */
    public ExportConfig<T> exclude(String... fieldNames) {
        this.excludeFields.addAll(Arrays.asList(fieldNames));
        return this;
    }

    /**
     * 设置Sheet名称
     */
    public ExportConfig<T> sheetName(String name) {
        this.sheetName = name;
        return this;
    }

    /**
     * 设置是否自动列宽
     */
    public ExportConfig<T> autoWidth(boolean autoWidth) {
        this.autoWidth = autoWidth;
        return this;
    }

    /**
     * 自动扫描字段（基于注解或字段名）
     */
    public ExportConfig<T> autoScan() {
        Field[] fields = dataClass.getDeclaredFields();
        for (Field field : fields) {
            if (excludeFields.contains(field.getName())) {
                continue;
            }
            if (!headers.containsKey(field.getName())) {
                headers.put(field.getName(), camelToDisplayName(field.getName()));
            }
        }
        return this;
    }

    /**
     * 驼峰转显示名称
     */
    private String camelToDisplayName(String camelCase) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(c));
        }
        return result.toString();
    }

    /**
     * 获取表头映射
     */
    public Map<String, String> getHeaders() {
        if (headers.isEmpty()) {
            autoScan();
        }
        return Collections.unmodifiableMap(headers);
    }
}
