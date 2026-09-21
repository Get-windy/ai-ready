package cn.aiedge.base.util;

import cn.aiedge.base.annotation.DataMask;
import cn.aiedge.base.entity.SysFieldPermission;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * 数据脱敏 Jackson 序列化器（配合 {@link DataMask} 注解使用）。
 *
 * <p>生效规则（优先级从高到低）：</p>
 *
 * <ol>
 *   <li><b>角色配置</b>（{@code sys_field_permission}，管理员在「岗位权限 → 敏感信息保护」维护）：
 *     查当前用户所属角色对该「表.字段」的配置
 *     <ul>
 *       <li>{@code visible = 0} —— 判定为不可见，输出 null（字段值不下发）</li>
 *       <li>{@code visible = 1} 且 {@code maskType != NONE} —— 按配置的打码方式与保留位数打码</li>
 *       <li>{@code visible = 1} 且 {@code maskType = NONE} —— 视为「可见且不打码」</li>
 *     </ul>
 *   </li>
 *   <li><b>注解默认值</b>：无角色配置时，按 {@link DataMask} 注解自身声明的类型/掩码字符处理</li>
 * </ol>
 *
 * <p><b>注意</b>：{@code @DataMask} 必须声明 {@code table}（所属表名），否则无法与角色配置匹配，
 * 该字段就只能是「注解默认脱敏」。属性名由 {@code createContextual} 从 {@link BeanProperty} 取得，
 * 二者共同构成配置的匹配键。</p>
 *
 * <p>配置读取失败（未登录、容器未就绪、查库异常）一律降级为「无配置」——
 * 脱敏链路的异常绝不能导致整个响应序列化失败。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class DataMaskSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private final DataMask annotation;

    /** 当前属性名，与注解的 table 一起构成 sys_field_permission 的匹配键 */
    private final String fieldName;

    /** 默认构造（Jackson 反序列化用） */
    public DataMaskSerializer() {
        this(null, null);
    }

    public DataMaskSerializer(DataMask annotation, String fieldName) {
        this.annotation = annotation;
        this.fieldName = fieldName;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        // ① 角色配置优先
        SysFieldPermission config = annotation == null
                ? null
                : FieldPermissionCache.findForCurrentUser(annotation.table(), fieldName);
        if (config != null) {
            if (config.getVisible() != null && config.getVisible() == 0) {
                // 配置为不可见：不下发该字段的值
                gen.writeNull();
                return;
            }
            if (isMasking(config.getMaskType())) {
                gen.writeString(maskByConfig(value, config));
                return;
            }
        }

        // ② 无配置（或配置为「可见且不打码」）→ 按注解默认值
        gen.writeString(mask(value, annotation));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) {
        if (property == null) {
            return this;
        }
        DataMask ann = property.getAnnotation(DataMask.class);
        if (ann == null) {
            ann = property.getContextAnnotation(DataMask.class);
        }
        if (ann == null) {
            return this;
        }
        return new DataMaskSerializer(ann, property.getName());
    }

    /**
     * 按注解配置脱敏（无角色配置时的默认行为）。
     */
    public static String mask(String value, DataMask annotation) {
        if (value == null || value.isEmpty() || annotation == null) {
            return value;
        }
        return maskByType(value, annotation.type(), charOf(annotation.maskChar()),
                annotation.maskPrefixLen(), annotation.maskSuffixLen());
    }

    /**
     * 按角色配置脱敏（配置里带打码方式与保留位数）。
     */
    private static String maskByConfig(String value, SysFieldPermission config) {
        int prefixLen = config.getMaskPrefixLen() == null ? 0 : config.getMaskPrefixLen();
        int suffixLen = config.getMaskSuffixLen() == null ? 0 : config.getMaskSuffixLen();
        return maskByType(value, config.getMaskType(), charOf(config.getMaskChar()), prefixLen, suffixLen);
    }

    /**
     * 打码分发。
     *
     * <p>PHONE / EMAIL / ID_CARD / BANK_CARD 用固定规则（各自格式是确定的）；
     * CUSTOM 才使用配置里的保留位数。
     * 前端「敏感信息保护」页的「效果预览」列按同一规则计算，改这里要同步改前端
     * （frontend/apps/pc-admin/src/views/system/role/components/RoleFieldPermissionTab.vue 的 maskPreview）。</p>
     */
    private static String maskByType(String value, String type, char maskChar, int prefixLen, int suffixLen) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String normalized = type == null ? "NONE" : type.toUpperCase();
        return switch (normalized) {
            case "PHONE" -> maskPhone(value, maskChar);
            case "EMAIL" -> maskEmail(value, maskChar);
            case "ID_CARD" -> maskIdCard(value, maskChar);
            case "BANK_CARD" -> maskBankCard(value, maskChar);
            case "CUSTOM" -> maskCustom(value, maskChar, prefixLen, suffixLen);
            default -> value;
        };
    }

    /** 该打码方式是否真的需要打码（空/NONE 表示不打码） */
    private static boolean isMasking(String maskType) {
        return maskType != null && !maskType.isEmpty() && !"NONE".equalsIgnoreCase(maskType);
    }

    /** 掩码字符：配置或注解里缺失时回退到 '*' */
    private static char charOf(String maskChar) {
        return (maskChar == null || maskChar.isEmpty()) ? '*' : maskChar.charAt(0);
    }

    private static String maskPhone(String value, char c) {
        if (value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + repeatChar(c, 4) + value.substring(7);
    }

    private static String maskEmail(String value, char c) {
        int atIdx = value.indexOf('@');
        if (atIdx <= 1) {
            return value;
        }
        return value.substring(0, 1) + repeatChar(c, Math.min(atIdx - 1, 4)) + value.substring(atIdx);
    }

    private static String maskIdCard(String value, char c) {
        if (value.length() < 10) {
            return value;
        }
        return value.substring(0, 6) + repeatChar(c, value.length() - 10) + value.substring(value.length() - 4);
    }

    private static String maskBankCard(String value, char c) {
        if (value.length() < 8) {
            return value;
        }
        return value.substring(0, 4) + repeatChar(c, value.length() - 8) + value.substring(value.length() - 4);
    }

    private static String maskCustom(String value, char c, int prefixLen, int suffixLen) {
        if (prefixLen + suffixLen >= value.length()) {
            return repeatChar(c, value.length());
        }
        return value.substring(0, prefixLen)
                + repeatChar(c, value.length() - prefixLen - suffixLen)
                + (suffixLen > 0 ? value.substring(value.length() - suffixLen) : "");
    }

    private static String repeatChar(char c, int count) {
        return String.valueOf(c).repeat(Math.max(0, count));
    }
}
