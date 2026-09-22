package cn.aiedge.agreement.dto;

import lombok.Data;

import java.util.List;

/**
 * 「**按业务时点取到的生效设定**」的下发形态（{@code AgreementRuntime.resolve} 的只读视图）。
 *
 * <p>给两类人用：</p>
 * <ol>
 *   <li>业务/客服："这笔单当时按的是哪一版、约定了什么" —— 直接可展示；</li>
 *   <li>下游链路调试：接入订单路由 / 结算之前先用它核对取值，避免"接错了字段还看不出来"。</li>
 * </ol>
 */
@Data
public class AgreementEffectiveSettingsVO {

    /** 该业务时点有没有生效版本；false 时 absenceReason 说明原因 */
    private Boolean versionFound;

    private String absenceReason;

    private Long agreementId;

    private Long versionId;

    private Integer versionNo;

    /** 查询用的业务时点 */
    private String businessTime;

    private String effectiveFrom;

    private String effectiveTo;

    /** 设定项（**含"未约定"的项**，未约定不会显示成 0） */
    private List<AgreementSettingItemVO> settings;

    /** 履约方式集合（可多选并存）；空集 = 本版未约定履约方式 */
    private List<AgreementFulfillmentModeVO> fulfillmentModes;
}
