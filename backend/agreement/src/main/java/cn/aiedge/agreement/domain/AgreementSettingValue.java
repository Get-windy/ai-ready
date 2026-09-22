package cn.aiedge.agreement.domain;

import cn.aiedge.agreement.entity.AgreementSetting;
import cn.aiedge.agreement.entity.AgreementSettingDef;
import cn.aiedge.agreement.enums.AgreementSettingValueType;
import cn.aiedge.common.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 一个设定项的**取值结果** —— 用三态表达，<b>不用 {@code null} 混同三件事</b>。
 *
 * <h3>为什么必须有三态（这是本类存在的唯一理由）</h3>
 * 下游链路（订单路由 / 结算 / 开票 / 风控……）要问的核心问题不是"值是多少"，
 * 而是"<b>双方到底约定了没有</b>"：
 * <table border="1">
 *   <tr><th>状态</th><th>含义</th><th>下游应当怎么做</th></tr>
 *   <tr><td>{@link State#AGREED}</td><td>**已约定**，本版里有这一项且值有效</td><td>按约定自动执行</td></tr>
 *   <tr><td>{@link State#UNDECLARED}</td><td>**未约定**：字段存在（平台定义了它），但本版没约定</td>
 *       <td><b>不自动执行、挂人工</b>，并明确报错（㉜）</td></tr>
 *   <tr><td>{@link State#UNDEFINED}</td><td>该字段**未定义**：平台字典里没有这个编码或已停用</td>
 *       <td>说明是配置/版本问题，通知平台维护，而不是当作"未约定"</td></tr>
 * </table>
 *
 * <p><b>为什么"未约定"和"未定义"要分开</b>：两者的处置完全不同 ——
 * 未约定是**双方的事**（要他们补约定），未定义是**平台的事**（字典里没这个字段，
 * 或者消费方用了旧的字段编码）。混成一个 {@code null} 会让排查方向直接跑偏。</p>
 *
 * <p>⚠️ 本类**没有任何 {@code orElse(默认值)} 的入口**，也不提供 {@code defaultValue}。
 * 拿不到"已约定"就只能是"未约定/未定义"，让调用方必须显式处理（㉜ / §13.3）。</p>
 */
public final class AgreementSettingValue {

    /** 三态（见类注释的表格）。 */
    public enum State {

        /** 已约定：本版里有这一项，且值有效。 */
        AGREED("已约定"),

        /** 未约定：字段存在，但本版没约定这一项 —— 下游不自动执行、挂人工。 */
        UNDECLARED("未约定"),

        /** 未定义：平台字段字典里没有这个编码（或已停用）—— 配置问题，不是双方的问题。 */
        UNDEFINED("未定义");

        private final String label;

        State(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final String settingKey;

    /** 中文名（取不到元数据时退化为编码本身，保证报错文案里总有个能搜的东西） */
    private final String label;

    private final State state;

    private final AgreementSettingValueType valueType;

    private final String text;

    private final BigDecimal number;

    private final Boolean bool;

    private final LocalDateTime date;

    /** ENUM 的候选值（来自元数据，供界面渲染下拉） */
    private final List<String> enumOptions;

    /** "改了这一项会影响什么"（来自元数据的语义说明） */
    private final String semantics;

    /** 消费方编码（{@code AgreementRuntime.ConsumerPoint}），界面据此显示"这一项会影响什么" */
    private final String consumerPoint;

    private AgreementSettingValue(String settingKey, String label, State state,
                                  AgreementSettingValueType valueType,
                                  String text, BigDecimal number, Boolean bool, LocalDateTime date,
                                  List<String> enumOptions, String semantics, String consumerPoint) {
        this.settingKey = settingKey;
        this.label = label;
        this.state = state;
        this.valueType = valueType;
        this.text = text;
        this.number = number;
        this.bool = bool;
        this.date = date;
        this.enumOptions = enumOptions == null ? List.of() : List.copyOf(enumOptions);
        this.semantics = semantics;
        this.consumerPoint = consumerPoint;
    }

    // ══════════════════════ 三个工厂（三态各一个，没有第四个） ══════════════════════

    /** 已约定（值来自本版那一行）。 */
    public static AgreementSettingValue agreed(AgreementSettingDef def, AgreementSetting row) {
        return new AgreementSettingValue(
                row.getSettingKey() == null ? def.getSettingKey() : row.getSettingKey(),
                def.getLabel() == null ? def.getSettingKey() : def.getLabel(),
                State.AGREED,
                valueTypeOf(row.getValueType(), def.getValueType()),
                row.getValueText(), row.getValueNumber(), row.getValueBool(), row.getValueDate(),
                splitOptions(def.getOptions()), def.getSemantics(), def.getConsumerPoint());
    }

    /**
     * 已约定，但值来自**别处**（不是 {@code agreement_setting} 那一行）。
     *
     * <p>目前唯一的用处是「履约方式集合」：它是集合语义、值在
     * {@code agreement_fulfillment_mode}（一版多行并存），这里只做**展示与消费方登记**的统一。</p>
     */
    public static AgreementSettingValue agreedText(AgreementSettingDef def, String text) {
        return new AgreementSettingValue(def.getSettingKey(),
                def.getLabel() == null ? def.getSettingKey() : def.getLabel(),
                State.AGREED, valueTypeOf(null, def.getValueType()),
                text, null, null, null,
                splitOptions(def.getOptions()), def.getSemantics(), def.getConsumerPoint());
    }

    /** 未约定（字段有定义，但本版没有这一行）。 */
    public static AgreementSettingValue undeclared(AgreementSettingDef def) {
        return new AgreementSettingValue(def.getSettingKey(),
                def.getLabel() == null ? def.getSettingKey() : def.getLabel(),
                State.UNDECLARED, valueTypeOf(null, def.getValueType()),
                null, null, null, null,
                splitOptions(def.getOptions()), def.getSemantics(), def.getConsumerPoint());
    }

    /** 未定义（平台字典里没有这个编码，或该编码已停用）。 */
    public static AgreementSettingValue undefined(String settingKey) {
        return new AgreementSettingValue(settingKey, settingKey, State.UNDEFINED, null,
                null, null, null, null, List.of(), null, null);
    }

    // ══════════════════════ 读取 ══════════════════════

    public String getSettingKey() {
        return settingKey;
    }

    public String getLabel() {
        return label;
    }

    public State getState() {
        return state;
    }

    public AgreementSettingValueType getValueType() {
        return valueType;
    }

    public List<String> getEnumOptions() {
        return enumOptions;
    }

    public String getSemantics() {
        return semantics;
    }

    public String getConsumerPoint() {
        return consumerPoint;
    }

    public boolean isAgreed() {
        return state == State.AGREED;
    }

    public boolean isUndeclared() {
        return state == State.UNDECLARED;
    }

    public boolean isUndefined() {
        return state == State.UNDEFINED;
    }

    /** 原始值（按类型取对应列），非"已约定"时为 {@code null}。仅供展示/序列化。 */
    public Object rawValue() {
        if (state != State.AGREED || valueType == null) {
            return null;
        }
        return switch (valueType) {
            case TEXT -> text;
            case NUMBER, DURATION -> number;
            case BOOL -> bool;
            case DATE -> date;
            case ENUM -> text;
        };
    }

    /**
     * 数值（含 DURATION 的天数）。
     *
     * @throws BusinessException 非"已约定"，或该字段不是数值型 —— 一律明确报错，
     *                           <b>不回退成 0</b>（回退一个数字就等于平台替双方做了决定）
     */
    public BigDecimal requireNumber() {
        assertAgreed("数值");
        if (number == null) {
            throw BusinessException.badRequest("本协议「" + label + "」虽已约定但没有有效数值，无法参与计算；请双方重新确认该设定");
        }
        return number;
    }

    /** 时长（天），DURATION 专用。 */
    public int requireDurationDays() {
        assertAgreed("时长");
        if (number == null) {
            throw BusinessException.badRequest("本协议「" + label + "」虽已约定但没有有效天数，无法推算日期；请双方重新确认该设定");
        }
        return number.intValue();
    }

    public String requireText() {
        assertAgreed("文本");
        if (text == null || text.isBlank()) {
            throw BusinessException.badRequest("本协议「" + label + "」虽已约定但没有有效内容；请双方重新确认该设定");
        }
        return text;
    }

    public Boolean requireBool() {
        assertAgreed("是/否");
        if (bool == null) {
            throw BusinessException.badRequest("本协议「" + label + "」虽已约定但没有有效取值；请双方重新确认该设定");
        }
        return bool;
    }

    public LocalDateTime requireDate() {
        assertAgreed("日期");
        if (date == null) {
            throw BusinessException.badRequest("本协议「" + label + "」虽已约定但没有有效日期；请双方重新确认该设定");
        }
        return date;
    }

    /** 断言已约定（供只想判"约定了没有"、不取值的调用方使用）。 */
    public void assertAgreed(String usage) {
        if (state == State.AGREED) {
            return;
        }
        throw BusinessException.badRequest(describeAbsence(usage));
    }

    /** 未约定 / 未定义的中文说明（"未约定"与"未定义"分开说，排查方向才不会跑偏）。 */
    public String describeAbsence(String usage) {
        String tail = usage == null || usage.isBlank() ? "" : "，无法" + usage;
        if (state == State.UNDECLARED) {
            return "本协议未约定「" + label + "」" + tail
                    + "。未约定的条款系统不会自动执行，请双方在协议里补充约定后再操作";
        }
        if (state == State.UNDEFINED) {
            return "字段「" + settingKey + "」未在平台字段字典中定义或已停用" + tail
                    + "，请联系平台维护字段元数据";
        }
        return "「" + label + "」状态异常，无法取值" + tail;
    }

    /** 给界面看的展示值（未约定/未定义时给出状态文字，而不是留一个空白让用户猜）。 */
    public String displayValue() {
        if (state != State.AGREED) {
            return state.getLabel();
        }
        if (valueType == null) {
            return "";
        }
        return switch (valueType) {
            case DURATION -> number == null ? "" : number.stripTrailingZeros().toPlainString() + " 天";
            case NUMBER -> number == null ? "" : number.stripTrailingZeros().toPlainString();
            case BOOL -> Boolean.TRUE.equals(bool) ? "是" : "否";
            case DATE -> date == null ? "" : date.toLocalDate().toString();
            case TEXT, ENUM -> text == null ? "" : text;
        };
    }

    @Override
    public String toString() {
        return "AgreementSettingValue{" + settingKey + "=" + state + ", " + displayValue() + "}";
    }

    // ── 内部 ──

    private static AgreementSettingValueType valueTypeOf(String rowType, String defType) {
        AgreementSettingValueType t = parseOrNull(rowType);
        return t != null ? t : parseOrNull(defType);
    }

    private static AgreementSettingValueType parseOrNull(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (AgreementSettingValueType t : AgreementSettingValueType.values()) {
            if (t.name().equalsIgnoreCase(name.trim())) {
                return t;
            }
        }
        return null;
    }

    private static List<String> splitOptions(String options) {
        if (options == null || options.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(options.split(","))
                .map(String::trim)
                .filter(Objects::nonNull)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
