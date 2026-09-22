package cn.aiedge.agreement.dto;

import lombok.Data;

import java.util.List;

/**
 * 保存协议内容的回执。
 *
 * <p>与草稿保存（{@code AgreementSaveResultVO}）同一条思路：点一次保存就要能看出
 * <b>距离"能生效"还差什么</b>，而不是点了生效才被拒。
 * ⚠️ 这里只做**提示**，不拦截：草稿阶段本来允许必填项未齐（那正是"洽谈中"的含义）。</p>
 *
 * <p>另附一句 {@code narrativeNotice}：文字条款不自动执行的口径，
 * 让前端在保存成功后的提示里也能带上（§13.2 要求界面显式告知）。</p>
 */
@Data
public class AgreementContentSaveResultVO {

    private Long agreementId;

    private Long versionId;

    /** 本次存下来的设定项数量（不含撤回的） */
    private Integer savedSettingCount;

    private Integer savedNarrativeCount;

    /** 本次约定的履约方式集合（中文名） */
    private List<String> fulfillmentModes;

    /** 必填但还没约定的字段编码 */
    private List<String> missingRequiredSettings;

    /** 必填但还没约定的字段中文名（给用户看的话术） */
    private List<String> missingRequiredSettingNames;

    /** 文字条款的固定提示（系统不会自动执行） */
    private String narrativeNotice;
}
