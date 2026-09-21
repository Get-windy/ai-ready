package cn.aiedge.base.annotation;

import cn.aiedge.base.util.NumberDataMaskSerializer;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数字型字段的脱敏标记（金额、成本价、折扣率、欠款等）。
 *
 * <p>与 {@link DataMask} 的区别：数字没有「部分打码」的通用形式
 * （把 12800.00 打成 12***.00 没有业务意义），因此本注解**只支持隐藏**：
 * 当前用户所属角色把该「表.字段」配成不可见时，序列化为 null；否则原样输出。</p>
 *
 * <p>必须声明 {@link #table()}，否则无法与 {@code sys_field_permission} 的配置匹配。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = NumberDataMaskSerializer.class)
@Documented
public @interface DataMaskNumber {

    /** 所属表名（如 biz_party），用于匹配 sys_field_permission 配置 */
    String table() default "";
}
