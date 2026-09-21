package cn.aiedge.base.annotation;

import cn.aiedge.base.util.DataMaskSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级脱敏标记。
 *
 * <p>打在业务实体的敏感字段上，序列化时由 {@link DataMaskSerializer} 处理。
 * 实际是否脱敏、脱成什么样，**优先取 {@code sys_field_permission} 里当前用户所属角色的配置**
 * （管理员在「岗位权限 → 敏感信息保护」里维护）；无配置时才用本注解的默认值。</p>
 *
 * <p><b>必须声明 {@link #table()}</b>（字段所属表名），否则无法与角色配置匹配，
 * 该字段就只能是「注解默认脱敏」，管理员在界面上配了也不会生效。</p>
 *
 * <p>本注解必须放在 core-base：业务模块（erp/* 等）只依赖 core-base，
 * 放在 core-api 会导致业务实体根本用不上。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DataMaskSerializer.class)
@Documented
public @interface DataMask {

    /** 脱敏类型：NONE/PHONE/EMAIL/ID_CARD/BANK_CARD/CUSTOM（无角色配置时的默认值） */
    String type() default "NONE";

    /** 掩码字符，默认 '*' */
    String maskChar() default "*";

    /** CUSTOM 类型保留的前缀长度 */
    int maskPrefixLen() default 0;

    /** CUSTOM 类型保留的后缀长度 */
    int maskSuffixLen() default 0;

    /** 所属表名（如 biz_party），用于匹配 sys_field_permission 配置 */
    String table() default "";
}
