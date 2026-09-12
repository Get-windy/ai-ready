package cn.aiedge.erp.sale.saleexchange.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * 宽松的 LocalDateTime 反序列化器：兼容前端日期控件可能传来的三种写法。
 *
 * <ul>
 *   <li>仅日期：{@code 2026-09-10} → 当日 00:00:00</li>
 *   <li>ISO 日期时间：{@code 2026-09-10T00:00:00} / 空格分隔</li>
 *   <li>带时区/毫秒：{@code 2026-09-10T00:00:00.000+08:00}</li>
 * </ul>
 *
 * <p>若不兼容「仅日期」，单据日期这类 date 控件字段会在 POST 时直接 400
 * （Cannot deserialize value of type LocalDateTime from String "2026-09-10"）。</p>
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        try {
            if (value.length() <= 10) {
                return LocalDate.parse(value).atStartOfDay();
            }
            if (value.length() >= 11 && value.charAt(10) == ' ') {
                value = value.substring(0, 10) + "T" + value.substring(11);
            }
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ignored) {
                return OffsetDateTime.parse(value).toLocalDateTime();
            }
        } catch (DateTimeParseException e) {
            throw new IOException("无法解析日期时间: " + text, e);
        }
    }
}
