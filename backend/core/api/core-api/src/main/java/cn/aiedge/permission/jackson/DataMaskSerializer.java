package cn.aiedge.permission.jackson;

import cn.aiedge.permission.annotation.DataMask;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

/**
 * 数据脱敏 Jackson 序列化器
 * <p>
 * 配合 {@link DataMask} 注解使用，在 JSON 序列化时自动对敏感字段进行脱敏。
 * 动态检测当前用户的字段级权限配置，若无脱敏配置则按注解定义执行脱敏。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class DataMaskSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private DataMask annotation;

    /** 默认构造（Jackson 反序列化用） */
    public DataMaskSerializer() {
    }

    public DataMaskSerializer(DataMask annotation) {
        this.annotation = annotation;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String masked = mask(value, annotation);
        gen.writeString(masked);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) {
        if (property != null) {
            DataMask ann = property.getAnnotation(DataMask.class);
            if (ann == null) {
                ann = property.getContextAnnotation(DataMask.class);
            }
            if (ann != null) {
                return new DataMaskSerializer(ann);
            }
        }
        return this;
    }

    /**
     * 根据注解配置对原始值执行脱敏
     */
    public static String mask(String value, DataMask annotation) {
        if (value == null || value.isEmpty() || annotation == null) {
            return value;
        }

        return switch (annotation.type().toUpperCase()) {
            case "PHONE" -> maskPhone(value, annotation);
            case "EMAIL" -> maskEmail(value, annotation);
            case "ID_CARD" -> maskIdCard(value, annotation);
            case "BANK_CARD" -> maskBankCard(value, annotation);
            case "CUSTOM" -> maskCustom(value, annotation);
            default -> value;
        };
    }

    private static String maskPhone(String value, DataMask annotation) {
        if (value.length() < 7) return value;
        return value.substring(0, 3) + repeatChar(annotation.maskChar().charAt(0), 4) + value.substring(7);
    }

    private static String maskEmail(String value, DataMask annotation) {
        int atIdx = value.indexOf('@');
        if (atIdx <= 1) return value;
        String name = value.substring(0, atIdx);
        String domain = value.substring(atIdx);
        char c = annotation.maskChar().charAt(0);
        return name.charAt(0) + repeatChar(c, Math.min(name.length() - 1, 4)) + domain;
    }

    private static String maskIdCard(String value, DataMask annotation) {
        if (value.length() < 10) return value;
        return value.substring(0, 6) + repeatChar(annotation.maskChar().charAt(0), value.length() - 10) + value.substring(value.length() - 4);
    }

    private static String maskBankCard(String value, DataMask annotation) {
        if (value.length() < 8) return value;
        return value.substring(0, 4) + repeatChar(annotation.maskChar().charAt(0), value.length() - 8) + value.substring(value.length() - 4);
    }

    private static String maskCustom(String value, DataMask annotation) {
        int prefixLen = annotation.maskPrefixLen();
        int suffixLen = annotation.maskSuffixLen();
        if (prefixLen + suffixLen >= value.length()) {
            return repeatChar(annotation.maskChar().charAt(0), value.length());
        }
        return value.substring(0, prefixLen)
                + repeatChar(annotation.maskChar().charAt(0), value.length() - prefixLen - suffixLen)
                + value.substring(value.length() - suffixLen);
    }

    private static String repeatChar(char c, int count) {
        return String.valueOf(c).repeat(Math.max(0, count));
    }

    // ==================== 工具方法：从字段权限配置中查找脱敏规则 ====================

    /**
     * 根据角色字段权限配置判断是否需要对指定表的字段脱敏
     *
     * @param permissions 当前用户的字段权限列表
     * @param tableName   表名
     * @param fieldName   字段名
     * @return 脱敏字符（空 Optional 表示不脱敏）
     */
    public static Optional<String> getMaskConfig(
            java.util.List<cn.aiedge.base.entity.SysFieldPermission> permissions,
            String tableName, String fieldName) {
        if (permissions == null || permissions.isEmpty()) return Optional.empty();

        return permissions.stream()
                .filter(p -> tableName.equals(p.getTargetTable())
                        && fieldName.equals(p.getTargetField())
                        && p.getStatus() == 1
                        && p.getVisible() == 0)
                .findFirst()
                .map(p -> p.getMaskType() != null && !p.getMaskType().isEmpty()
                        ? p.getMaskType() : "HIDE");
    }
}
