package cn.aiedge.agreement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 受邀方**领取并查看**（§13.5）。
 *
 * <p>两个入口任选其一：{@code token}（从链接 / 二维码扫出来的那串）或
 * {@code inviteCode}（人工口述的短码）。两者都受同一套五绑定校验约束。</p>
 *
 * <p>{@code representedPartyId} 必填：打开者要明确"我代表哪个主体"。
 * 这个声明**必须**与邀请绑定的目标主体一致，否则一律回「这份契约不是发给你的」——
 * 转发给别人打不开，正是靠这一条（会话租户 + 所代表主体双匹配）实现的。</p>
 */
@Data
public class AgreementInviteOpenDTO {

    @Schema(description = "链接/二维码里的 token（明文）。与 inviteCode 二选一")
    private String token;

    @Schema(description = "人读邀请码（口头/人工输入）。与 token 二选一")
    private String inviteCode;

    @Schema(description = "打开者声明的代表主体 ID（必须与邀请绑定的目标主体一致）")
    private Long representedPartyId;

    @Schema(description = "打开渠道：QRCODE / LINK；不传按 LINK 记（用于领取留痕）")
    private String channel;
}
