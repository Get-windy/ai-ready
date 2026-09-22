package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.enums.AgreementSettingValueType;
import cn.aiedge.common.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 设定值的**解析与强类型校验**（纯函数，可单测）。
 *
 * <p>前端只提交一个字符串 {@code value}（按字段类型输入），解析与校验一律在这里做，
 * 原因与"字典只定义选项、不给默认值"同源：<b>口径只允许有一处</b>。
 * 若让每个界面各写一份解析，迟早出现"这里能存进去、那里读不出来"的字段。</p>
 *
 * <h3>校验失败一律中文业务异常</h3>
 * 报错里带上字段中文名与原因，用户可以自己改；用 RuntimeException 会被兜底 advice
 * 吞成 HTTP 500「系统异常，请稍后重试」，把可自解的问题误导成服务故障（本仓实踩）。</p>
 *
 * <p><b>⚠️ 本类不提供任何"解析不出来就用默认值"的分支</b>：解析失败就是拒绝保存。
 * 未约定是"没有这一行"，不是"这一行的值被兜底成了某个数"（㉜）。</p>
 */
public final class AgreementSettingRules {

    private static final DateTimeFormatter DATE_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private AgreementSettingRules() {
    }

    /**
     * 一个设定项的**强类型值**（哪种类型用哪一列，与 {@code agreement_setting} 的分列存法一致）。
     */
    public record TypedValue(String valueType, String text, BigDecimal number, Boolean bool, LocalDateTime date) {

        /** 是否"空值"：前端显式提交空串表示"撤回这一项的约定"。 */
        public boolean isEmpty() {
            return isEmpty(text) && number == null && bool == null && date == null;
        }

        private static boolean isEmpty(String s) {
            return s == null || s.isBlank();
        }
    }

    /**
     * 校验字段编码对应的元数据可用（存在、启用、类型合法、消费方合法）。
     *
     * @throws BusinessException 字段未定义 / 已停用 / 取值类型非法 / 缺消费方
     */
    public static void assertDefUsable(AgreementSettingDef def, String settingKey) {
        if (def == null) {
            throw BusinessException.badRequest("字段「" + settingKey
                    + "」未在平台字段字典中定义，不能写入协议；请先在平台维护该字段");
        }
        if (def.getStatus() != null && def.getStatus() != 1) {
            throw BusinessException.badRequest("字段「" + def.getLabel() + "」已在平台字段字典中停用，"
                    + "新协议不能再约定它（历史协议不受影响）");
        }
        AgreementSettingValueType type = parseType(def.getValueType());
        if (type == null) {
            throw BusinessException.badRequest("字段「" + def.getLabel() + "」的取值类型「" + def.getValueType()
                    + "」非法，请联系平台维护");
        }
        if (def.getConsumerPoint() == null || def.getConsumerPoint().isBlank()) {
            // §13.3 的硬要求：没有消费方的字段不许进设定版（迁移里的 DO $$ 也会拦住这一条）
            throw BusinessException.badRequest("字段「" + def.getLabel()
                    + "」未登记消费方（这个字段被谁用），按平台规矩不能进协议设定版；请联系平台维护");
        }
        if (AgreementRuntime.ConsumerPoint.of(def.getConsumerPoint()) == null) {
            throw BusinessException.badRequest("字段「" + def.getLabel() + "」登记的消费方「" + def.getConsumerPoint()
                    + "」不在平台允许的下游环节清单里，请联系平台维护");
        }
    }

    /** 取值类型（元数据里存的字符串 → 枚举），非法时返回 null（由调用方给出中文报错）。 */
    public static AgreementSettingValueType parseType(String valueType) {
        if (valueType == null || valueType.isBlank()) {
            return null;
        }
        for (AgreementSettingValueType t : AgreementSettingValueType.values()) {
            if (t.name().equalsIgnoreCase(valueType.trim())) {
                return t;
            }
        }
        return null;
    }

    /**
     * 按元数据的取值类型解析并校验一个字符串值。
     *
     * @param rawValue 前端提交的原始值；{@code null} / 空白 ⇒ 返回"空值"（调用方据此删除该行 = 回到未约定）
     * @throws BusinessException 取值不符合该字段的类型 / 候选值约束
     */
    public static TypedValue parse(AgreementSettingDef def, String rawValue) {
        AgreementSettingValueType type = parseType(def.getValueType());
        assertDefUsable(def, def.getSettingKey());
        String label = def.getLabel() == null ? def.getSettingKey() : def.getLabel();
        if (rawValue == null || rawValue.isBlank()) {
            return new TypedValue(type.name(), null, null, null, null);
        }
        String raw = rawValue.trim();
        if (type == AgreementSettingValueType.ENUM) {
            // 枚举值要与元数据里的候选值**逐字**一致（大小写不敏感后取候选值原样）
            return new TypedValue(type.name(), parseEnum(def, label, raw), null, null, null);
        }
        return switch (type) {
            case TEXT -> new TypedValue(type.name(), raw, null, null, null);
            case BOOL -> new TypedValue(type.name(), null, null, parseBool(label, raw), null);
            case NUMBER -> new TypedValue(type.name(), null, parseNumber(label, raw), null, null);
            case DURATION -> new TypedValue(type.name(), null, parseDays(label, raw), null, null);
            case DATE -> new TypedValue(type.name(), null, null, null, parseDate(label, raw));
            // ENUM 已在上面单独处理（要与候选值逐字一致）
            case ENUM -> new TypedValue(type.name(), parseEnum(def, label, raw), null, null, null);
        };
    }

    /** 校验一个"已有值"是否合法（用于模板复制、数据体检等场景）。 */
    public static void assertValueUsable(AgreementSettingDef def, String valueType, String text,
                                         BigDecimal number, Boolean bool, LocalDateTime date) {
        AgreementSettingValueType type = parseType(valueType);
        if (type == null) {
            type = parseType(def.getValueType());
        }
        String label = def == null || def.getLabel() == null ? (def == null ? "" : def.getSettingKey()) : def.getLabel();
        if (type == null) {
            throw BusinessException.badRequest("字段「" + label + "」的取值类型缺失，无法校验");
        }
        switch (type) {
            case ENUM -> {
                if (text == null || text.isBlank()) {
                    throw BusinessException.badRequest("字段「" + label + "」是枚举项，必须有取值");
                }
                parseEnum(def, label, text);
            }
            case TEXT -> {
                if (text == null || text.isBlank()) {
                    throw BusinessException.badRequest("字段「" + label + "」是文本项，内容不能为空");
                }
            }
            case BOOL -> {
                if (bool == null) {
                    throw BusinessException.badRequest("字段「" + label + "」是「是/否」项，取值不能为空");
                }
            }
            case NUMBER, DURATION -> {
                if (number == null) {
                    throw BusinessException.badRequest("字段「" + label + "」是数值项，取值不能为空");
                }
                if (type == AgreementSettingValueType.DURATION && number.scale() > 0) {
                    throw BusinessException.badRequest("字段「" + label + "」是天数，只能是整数天");
                }
            }
            case DATE -> {
                if (date == null) {
                    throw BusinessException.badRequest("字段「" + label + "」是日期项，取值不能为空");
                }
            }
        }
    }

    // ── 各类型解析 ──

    private static String parseEnum(AgreementSettingDef def, String label, String raw) {
        List<String> options = options(def);
        if (options.isEmpty()) {
            throw BusinessException.badRequest("字段「" + label + "」是枚举项但没有配置候选值，请联系平台维护");
        }
        for (String option : options) {
            if (option.equalsIgnoreCase(raw)) {
                return option;
            }
        }
        throw BusinessException.badRequest("字段「" + label + "」只能从下列取值中选择：" + String.join(" / ", options));
    }

    private static BigDecimal parseNumber(String label, String raw) {
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            // ⚠️ 这里绝不能"解析失败就置 0"：0 是一个真实约定值，与"没填对"是两件事
            throw BusinessException.badRequest("字段「" + label + "」必须填数字，当前填的是「" + raw + "」");
        }
    }

    private static BigDecimal parseDays(String label, String raw) {
        BigDecimal n;
        try {
            n = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw BusinessException.badRequest("字段「" + label + "」必须填天数（整数），当前填的是「" + raw + "」");
        }
        if (n.scale() > 0) {
            throw BusinessException.badRequest("字段「" + label + "」是天数，只能填整数天，当前填的是「" + raw + "」");
        }
        if (n.signum() < 0) {
            throw BusinessException.badRequest("字段「" + label + "」是天数，不能为负数");
        }
        return n;
    }

    private static Boolean parseBool(String label, String raw) {
        String v = raw.toLowerCase();
        if (Arrays.asList("true", "1", "是", "y", "yes").contains(v)) {
            return Boolean.TRUE;
        }
        if (Arrays.asList("false", "0", "否", "n", "no").contains(v)) {
            return Boolean.FALSE;
        }
        throw BusinessException.badRequest("字段「" + label + "」只能填「是」或「否」，当前填的是「" + raw + "」");
    }

    private static LocalDateTime parseDate(String label, String raw) {
        String v = raw.trim().replace('T', ' ');
        try {
            if (v.length() <= 10) {
                return LocalDate.parse(v, DATE_DAY).atStartOfDay();
            }
            // 允许 "yyyy-MM-dd HH:mm[:ss]"，秒可有可无
            String normalized = v.length() == 16 ? v + ":00" : v;
            return LocalDateTime.parse(normalized.replace(' ', 'T'));
        } catch (Exception e) {
            throw BusinessException.badRequest("字段「" + label + "」必须填日期（形如 2026-09-22 或 2026-09-22 09:30），"
                    + "当前填的是「" + raw + "」");
        }
    }

    /** ENUM 的候选值（元数据里逗号分隔）。 */
    public static List<String> options(AgreementSettingDef def) {
        if (def == null || def.getOptions() == null || def.getOptions().isBlank()) {
            return List.of();
        }
        return Arrays.stream(def.getOptions().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .filter(Objects::nonNull)
                .toList();
    }
}
