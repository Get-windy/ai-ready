package cn.aiedge.trade.monitor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 时间参数宽松解析（API 监控页查询条件）
 *
 * <p>页面统一发送 `yyyy-MM-dd HH:mm:ss`，但联调/脚本可能发送 ISO（`yyyy-MM-ddTHH:mm:ss`）或纯日期，
 * 故在此统一兼容；解析失败按「未传」处理（区间用今日兜底），**不抛 400**，避免因格式差异打挂查询。</p>
 */
public final class TimeParsers {

    private static final DateTimeFormatter SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter SPACE_MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TimeParsers() {
    }

    /** 宽松解析：yyyy-MM-dd HH:mm:ss / yyyy-MM-dd HH:mm / ISO / yyyy-MM-dd */
    public static LocalDateTime parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim().replace('T', ' ');
        if (value.endsWith("Z")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        int dot = value.indexOf('.');
        if (dot > 0) {
            value = value.substring(0, dot);
        }
        for (DateTimeFormatter formatter : new DateTimeFormatter[]{SPACE, SPACE_MINUTE}) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种格式
            }
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** 起始时间：未传 → 今日 00:00 */
    public static LocalDateTime startOfRange(String text) {
        LocalDateTime parsed = parse(text);
        return parsed != null ? parsed : LocalDate.now().atStartOfDay();
    }

    /** 结束时间：未传 → 明日 00:00；只传日期时补足为「当日结束」 */
    public static LocalDateTime endOfRange(String text) {
        LocalDateTime parsed = parse(text);
        if (parsed == null) {
            return LocalDate.now().plusDays(1).atStartOfDay();
        }
        // 传的是纯日期（00:00:00）时，按「含当日」处理
        if (parsed.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return parsed.plusDays(1);
        }
        return parsed;
    }
}
