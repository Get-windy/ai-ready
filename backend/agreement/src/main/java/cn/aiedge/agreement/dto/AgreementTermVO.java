package cn.aiedge.agreement.dto;

import lombok.Data;

/**
 * 协议条款（下发给前端的形态：编码 + 当时的名称/语义 + 参数）。
 *
 * <p>名称与语义来自**快照**而不是现查字典：字典日后被改，历史版本仍要能显示"当时约定了什么"。</p>
 */
@Data
public class AgreementTermVO {

    private Long id;

    private Long agreementId;

    private Long versionId;

    private String termCode;

    /** 条款类别中文名（当前实现取字典里的选项名，属展示用近似；确权以快照中的 optionLabel 为准） */
    private String termName;

    private String optionCode;

    private String optionLabel;

    /** 该选项的系统执行语义（选了它就等于约定系统这么执行） */
    private String semantics;

    /** 需要参数时的参数名与之和（needsParam 非空则 paramValue 必填） */
    private String needsParam;

    private String paramValue;

    /** 是否必填条款（来自字典；必填没选完不许生效） */
    private Boolean required;
}
