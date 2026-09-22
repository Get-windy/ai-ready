package cn.aiedge.agreement.dto;

import lombok.Data;

import java.util.List;

/**
 * 新建 / 修改契约模板的请求体。
 *
 * <h3>⚠️ 模板内容是"预填"，不是"默认值"（§13.9）</h3>
 * 这里的 {@code settings} / {@code terms} / {@code narratives} 是**发起时替你填好**的内容，
 * <b>不写入</b> {@code agreement_setting} 的"已约定"状态：
 * 从模板发起契约后，双方仍要在那一版上确认与双签。
 * "模板里有 ⇒ 视为已约定"是绝不允许的（那等于平台替双方定商业条款，㉜）。</p>
 *
 * <h3>修改时的覆盖口径</h3>
 * 三份清单都是**整份覆盖**：字段不传 = 不动；传了空数组 = 清空该部分
 * （用 {@code settings == null} 与 {@code List.of()} 区分"不动"与"清空"）。
 */
@Data
public class AgreementTemplateDTO {

    /** PLATFORM 平台模板 / TENANT 租户模板；平台模板只有平台侧能建（码 agreement:platform:template:manage） */
    private String scope;

    private String templateName;

    /** 适用协议类型：PLATFORM_SERVICE / DISTRIBUTION / GOODS_FRAMEWORK / CONSUMER_PROMISE */
    private String agreementType;

    private String description;

    /** 法务审核：PENDING 待审 / APPROVED 已通过 / REJECTED 判定违法（REJECTED 不许用于发起） */
    private String legalReviewStatus;

    /** 1=启用 / 0=停用 */
    private Integer status;

    /** 预填的设定项（结构与协议内容保存一致） */
    private List<AgreementContentSaveDTO.SettingItem> settings;

    /** 预填的条款选择（来自平台条款字典） */
    private List<TermSelectDTO> terms;

    /** 预填的文字条款 */
    private List<AgreementContentSaveDTO.NarrativeItem> narratives;
}
