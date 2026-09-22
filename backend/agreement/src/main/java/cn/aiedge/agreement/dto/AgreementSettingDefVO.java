package cn.aiedge.agreement.dto;

import lombok.Data;

import java.util.List;

/**
 * 字段元数据（下发前端）："协议里可以约定哪些字段、怎么填、<b>会影响什么</b>"。
 *
 * <p>⚠️ {@code consumerPoint} / {@code consumerPointLabel} / {@code semantics} 三项是**给人看的**：
 * 界面在字段旁边显示"这一项会影响订单路由"这类说明，用户改之前就知道后果。
 * 这也正是 §13.3 立"数据上必须有消费方"这条规矩的意义 ——
 * 没有消费方的字段不准进设定版，也就不会出现在这里。</p>
 *
 * <p>⚠️ 本 VO <b>没有</b> defaultValue 之类的字段，也不许加：
 * 平台给默认值 = 平台替双方做决定（㉜ / §3.4.4d1）。未约定就是未约定。</p>
 */
@Data
public class AgreementSettingDefVO {

    private Long id;

    private String settingKey;

    private String label;

    /** ENUM / NUMBER / TEXT / BOOL / DATE / DURATION */
    private String valueType;

    private String valueTypeLabel;

    /** ENUM 的候选值（界面渲染下拉）；非 ENUM 为空列表 */
    private List<String> options;

    /** 是否必填；未约定必填项时保存回执会列出来 */
    private Boolean required;

    /** 消费方编码（如 AR_DUE_DATE），界面可做"影响范围"标签 */
    private String consumerPoint;

    /** 消费方中文名（如「应收应付到期日」） */
    private String consumerPointLabel;

    /** 消费方语义说明（"这一项会影响什么"） */
    private String consumerSemantics;

    /** 字段自身说明 */
    private String semantics;

    private Integer sort;

    /** 1=启用 / 0=停用 */
    private Integer status;
}
