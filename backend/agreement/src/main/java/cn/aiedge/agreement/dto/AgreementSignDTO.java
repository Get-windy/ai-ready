package cn.aiedge.agreement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 签署（§13.6「**自然人代表主体**」）。
 *
 * <p>⚠️ 这里问的是"**谁签的、凭什么代表这家公司**" —— 只传一个账号 ID 是不够的：
 * 司法上要能回答签署人代表哪个主体、依据什么授权。
 * 其中 {@code representedPartyId} 与 {@code authorityBasis} <b>必填</b>。</p>
 *
 * <h3>与「确认（confirm）」的关系</h3>
 * 签署是**更强的动作**：服务端先复用既有的"本方确认"链路（同一套双签痕迹），
 * 再额外落一条签署记录。不存在两套互相打架的确认机制 ——
 * 库里的双签痕迹仍是"能否置为生效"的唯一依据。
 */
@Data
public class AgreementSignDTO {

    @Schema(description = "代表哪个主体签（必须与协议这一端的缔约主体一致）")
    private Long representedPartyId;

    @Schema(description = "自然人主体 ID（R1 自然人×往来单位，通用化前可为空；填了多一份举证）")
    private Long personId;

    @Schema(description = "签署人姓名（留痕，人读）")
    private String signerName;

    @Schema(description = "授权依据（凭什么代表这家主体签字），如「法定代表人本人」「授权委托书（含授权范围与限额）」")
    private String authorityBasis;

    @Schema(description = "授权凭证号（外部单据编号，如授权委托书编号）；可空")
    private String authorityEvidenceNo;

    @Schema(description = "签署渠道（留痕用），如 PC / 移动端")
    private String channel;

    @Schema(description = "备注")
    private String remark;
}
