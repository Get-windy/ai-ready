package cn.aiedge.agreement.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 契约模板详情（含预填的三部分内容）。
 *
 * <p>⚠️ 这里的 {@code settings} 是"模板里建议这么填"，**不是**某个版本的约定值 ——
 * 因此它们没有三态、也不带"是否已约定"的语义；一旦被用于发起契约，
 * 它们会被**预填**到新草稿上，由双方在那一版上确认（§13.9）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgreementTemplateDetailVO extends AgreementTemplateVO {

    private List<AgreementSettingItemVO> settings;

    private List<AgreementTermVO> terms;

    private List<AgreementNarrativeVO> narratives;
}
