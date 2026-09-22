package cn.aiedge.agreement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 保存协议内容（字段设定版 + 文字版 + 履约方式集合）。
 *
 * <h3>⚠️ 整份覆盖，不是增量合并</h3>
 * 三部分都是"把这一版改成现在提交的样子"：
 * <ul>
 *   <li>{@code settings} 里没出现的字段 ⇒ 回到「**未约定**」（不是"保持原值"）——
 *       这正是"未约定就不自动执行"的前提，用户清空一项必须真的清空；</li>
 *   <li>{@code settings} 里某项 {@code value} 留空 ⇒ 同样回到未约定；</li>
 *   <li>{@code fulfillmentModes} 不传或传空数组 ⇒ 这一版没有约定履约方式（空集，
 *       <b>不等于</b>"随便用哪种"）。</li>
 * </ul>
 *
 * <p>只允许保存到**草稿**版本：已生效版本只读（㉛，改协议只能发起变更后重新签署）。</p>
 */
@Data
public class AgreementContentSaveDTO {

    @Schema(description = "字段设定版：整份覆盖，未出现的字段视为未约定")
    private List<SettingItem> settings;

    @Schema(description = "文字版：整份覆盖，未出现的段落即删除")
    private List<NarrativeItem> narratives;

    @Schema(description = "履约方式集合（可多选并存）：整份覆盖，空 = 本版未约定履约方式")
    private List<String> fulfillmentModes;

    /** 一个设定项。 */
    @Data
    public static class SettingItem {

        @Schema(description = "字段编码，如 AR_CREDIT_DAYS")
        private String settingKey;

        /**
         * 取值（字符串形态，服务端按字段类型解析与校验）。
         * ENUM 填候选值编码 / NUMBER 填数字 / BOOL 填「是」或「否」/ DATE 填 2026-09-22 /
         * DURATION 填整数天。留空表示**撤回这一项的约定**。
         */
        @Schema(description = "取值；留空表示撤回这一项的约定（回到未约定）")
        private String value;

        private String remark;
    }

    /** 一段文字条款。 */
    @Data
    public static class NarrativeItem {

        @Schema(description = "段落类别：DISPUTE / CONFIDENTIALITY / FORCE_MAJEURE / SPECIAL_TERMS / BREACH_LIABILITY_TEXT")
        private String sectionCode;

        @Schema(description = "正文；留空表示不写这一段（等于删除）")
        private String contentText;
    }
}
