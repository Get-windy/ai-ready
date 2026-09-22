package cn.aiedge.agreement.dto;

import lombok.Data;

import java.util.List;

/**
 * 某个版本里一个设定项的**三态**取值（下发前端）。
 *
 * <p>⚠️ {@code state} 是关键字段，别把它当装饰：</p>
 * <ul>
 *   <li>{@code AGREED} 已约定 —— 界面显示值；</li>
 *   <li>{@code UNDECLARED} 未约定 —— 界面必须显示「未约定」并提示
 *       "系统不会按默认值执行，需要双方约定"（<b>不能显示成 0 或空</b>）；</li>
 *   <li>{@code UNDEFINED} 未定义 —— 该字段已不在平台字典里（配置问题），提示联系平台。</li>
 * </ul>
 */
@Data
public class AgreementSettingItemVO {

    private String settingKey;

    private String label;

    private String valueType;

    private String valueTypeLabel;

    /** AGREED 已约定 / UNDECLARED 未约定 / UNDEFINED 未定义 */
    private String state;

    private String stateLabel;

    /** 原始值（字符串形态，便于表单回填）；未约定时为空 */
    private String value;

    /** 展示值（DURATION 会带"天"、BOOL 会转成"是/否"） */
    private String displayValue;

    private Boolean required;

    private String consumerPoint;

    private String consumerPointLabel;

    private String semantics;

    private String remark;

    private List<String> options;
}
