package cn.aiedge.base.util;

import cn.aiedge.base.annotation.DataMaskNumber;
import cn.aiedge.base.entity.SysFieldPermission;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * 数字型敏感字段（金额/成本价/折扣率等）的脱敏序列化器，配合 {@link DataMaskNumber} 使用。
 *
 * <p>只有一个动作：当前用户所属角色把该「表.字段」配成不可见（{@code visible = 0}）时输出 null，
 * 否则原样输出。数字没有「部分打码」的通用形式，故不支持 maskType。</p>
 *
 * <p>配置读取失败一律降级为「按原值输出」，绝不因脱敏链路异常导致响应序列化失败。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
public class NumberDataMaskSerializer extends JsonSerializer<BigDecimal> implements ContextualSerializer {

    private final String table;

    /** 当前属性名，与 table 一起构成 sys_field_permission 的匹配键 */
    private final String fieldName;

    /** 默认构造（Jackson 反序列化用） */
    public NumberDataMaskSerializer() {
        this(null, null);
    }

    public NumberDataMaskSerializer(String table, String fieldName) {
        this.table = table;
        this.fieldName = fieldName;
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        SysFieldPermission config = FieldPermissionCache.findForCurrentUser(table, fieldName);
        if (config != null && config.getVisible() != null && config.getVisible() == 0) {
            // 配置为不可见：金额不下发（而不是给 0 —— 0 会被误读为「真的欠款为 0」）
            gen.writeNull();
            return;
        }
        gen.writeNumber(value);
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider provider, BeanProperty property) {
        if (property == null) {
            return this;
        }
        DataMaskNumber ann = property.getAnnotation(DataMaskNumber.class);
        if (ann == null) {
            ann = property.getContextAnnotation(DataMaskNumber.class);
        }
        if (ann == null) {
            return this;
        }
        return new NumberDataMaskSerializer(ann.table(), property.getName());
    }
}
