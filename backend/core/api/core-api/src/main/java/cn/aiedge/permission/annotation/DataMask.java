package cn.aiedge.permission.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.*;

/**
 * 数据脱敏注解
 * <p>
 * 标注在实体字段上，在 JSON 序列化时自动根据当前用户的字段级权限配置进行脱敏。
 * 支持内置脱敏类型（手机号、邮箱、身份证等）和自定义掩码模式。
 * </p>
 *
 * 使用示例：
 * <pre>{@code
 * @DataMask(type = "PHONE")
 * private String phone;
 *
 * @DataMask(type = "CUSTOM", maskChar = "#", maskPrefixLen = 2, maskSuffixLen = 2)
 * private String customField;
 * }</pre>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = cn.aiedge.permission.jackson.DataMaskSerializer.class)
@Documented
public @interface DataMask {

    /**
     * 脱敏类型
     * <ul>
     *   <li>NONE - 不脱敏</li>
     *   <li>PHONE - 手机号（保留前3后4）</li>
     *   <li>EMAIL - 邮箱（@前保留首字符）</li>
     *   <li>ID_CARD - 身份证（保留前6后4）</li>
     *   <li>BANK_CARD - 银行卡（保留前4后4）</li>
     *   <li>CUSTOM - 自定义（使用 maskChar/maskPrefixLen/maskSuffixLen）</li>
     * </ul>
     */
    String type() default "NONE";

    /**
     * 脱敏填充字符（仅 CUSTOM 类型有效）
     */
    String maskChar() default "*";

    /**
     * 保留前缀长度（仅 CUSTOM 类型有效）
     */
    int maskPrefixLen() default 0;

    /**
     * 保留后缀长度（仅 CUSTOM 类型有效）
     */
    int maskSuffixLen() default 0;

    /**
     * 目标表名（用于匹配 sys_field_permission 配置）
     * 为空时自动从实体类名推断
     */
    String table() default "";
}
