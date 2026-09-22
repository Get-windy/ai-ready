package cn.aiedge.agreement.enums;

import cn.aiedge.common.exception.BusinessException;

/**
 * 字段设定版的**取值类型**（{@code agreement_setting_def.value_type}）。
 *
 * <p>为什么设定版要**强类型**而不是一段 JSON：设定版是**运行时配置**、要被订单路由 / 发货 /
 * 库存 / 定价 / 结算 / 开票 / 风控直接消费（DOMAIN-MODEL §13.1）。若存成一坨自由文本，
 * 每个消费方都要自己解析一遍、自己决定"解析不出来怎么办"，
 * 于是"未约定"和"填错了"会被混成同一件事，㉜「未约定就不自动执行」就守不住了。</p>
 *
 * <p>强类型的落法：{@code agreement_setting} 一张表里按类型分列存
 * （{@code value_text} / {@code value_number} / {@code value_bool} / {@code value_date}），
 * 读的时候由本类型决定读哪一列 —— 单一事实来源，不靠消费方猜。</p>
 */
public enum AgreementSettingValueType {

    /** 枚举单选（候选值见 {@code agreement_setting_def.options}，逗号分隔） */
    ENUM("枚举单选"),

    /** 数值（金额 / 比例 / 天数等，十进制，比较用 BigDecimal 避免浮点误差） */
    NUMBER("数值"),

    /** 文本（区域、备注性描述等，不参与自动计算的自由文本） */
    TEXT("文本"),

    /** 是 / 否 */
    BOOL("是/否"),

    /** 日期（含时刻，存 {@code value_date}） */
    DATE("日期"),

    /** 时长（以**天**为单位存 {@code value_number}），如结算周期 */
    DURATION("时长（天）");

    private final String label;

    AgreementSettingValueType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 该类型是否以「天」为单位（DURATION）。展示与消费时用它决定要不要补"天"这个单位。 */
    public boolean isDayCount() {
        return this == DURATION;
    }

    /**
     * 解析前端传入的类型名（大小写不敏感）。
     *
     * @throws BusinessException 为空或不在枚举内（中文文案；不用 RuntimeException，
     *                           否则会被兜底 advice 吞成 HTTP 500「系统异常」，把可自解的问题误导成故障）
     */
    public static AgreementSettingValueType parse(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw BusinessException.badRequest("请选择字段取值类型");
        }
        String n = name.trim();
        for (AgreementSettingValueType t : values()) {
            if (t.name().equalsIgnoreCase(n)) {
                return t;
            }
        }
        throw BusinessException.badRequest("字段取值类型「" + name + "」不支持；可选：ENUM / NUMBER / TEXT / BOOL / DATE / DURATION");
    }
}
