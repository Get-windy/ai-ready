package cn.aiedge.agreement.dto;

import lombok.Data;

import java.util.List;

/**
 * 「某版本的协议内容」：**字段设定版 + 文字版 + 履约方式集合**（一次取全）。
 *
 * <p>三条腿为什么放在同一个返回里：它们本来就是"同一版协议"的三个部分
 * （§13.1：设定版是可执行内核、文字版是权责与举证），分开取会让前端出现
 * "设定读到了、文字没读到"的中间态，用户看到的内容是残缺的。</p>
 */
@Data
public class AgreementContentVO {

    private Long agreementId;

    private Long versionId;

    private Integer versionNo;

    /** DRAFT / ACTIVE / SUPERSEDED / REJECTED */
    private String versionStatus;

    private String versionStatusLabel;

    /** 本版内容现在能不能改（只有草稿能改；已生效版本只读 —— ㉛） */
    private Boolean editable;

    /** 不能改时的一句话说明（直接展示给用户） */
    private String editableHint;

    /** 字段设定版（**含"未约定"的项**：界面要能看出哪些还没约定） */
    private List<AgreementSettingItemVO> settings;

    /** 必填但还没约定的字段编码 */
    private List<String> missingRequiredSettings;

    /** 必填但还没约定的字段中文名 */
    private List<String> missingRequiredSettingNames;

    /** 文字版（不自动执行，只留痕举证） */
    private List<AgreementNarrativeVO> narratives;

    /** 履约方式集合（可多选并存） */
    private List<AgreementFulfillmentModeVO> fulfillmentModes;

    /** 不含未约定的项，仅便于前端做"已约定摘要" */
    private List<String> agreedSettingKeys;

    /** 字段元数据（界面据此渲染表单并显示"这一项会影响什么"） */
    private List<AgreementSettingDefVO> settingDefs;

    /** 从模板发起时的来源留痕（如「基于模板「标准代销模板」（平台模板）起草，第 1 版」）；从零起草为空 */
    private String templateSourceText;
}
