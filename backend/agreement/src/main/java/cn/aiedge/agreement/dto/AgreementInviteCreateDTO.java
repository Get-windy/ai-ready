package cn.aiedge.agreement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发起**唯一送达**（§13.5）。
 *
 * <p>⚠️ 前端**不需要**（也不允许）指定"发给哪一方"：收件方由服务端按会话租户判定 ——
 * 谁发起就发给**另一端**。让前端传"我发给谁"等于把"这份契约不是发给你的"这条判定
 * 交给客户端，唯一送达就形同虚设。</p>
 */
@Data
public class AgreementInviteCreateDTO {

    @Schema(description = "绑定到哪一版文稿；不传 = 当前待双方确认的那个草稿版本")
    private Long versionId;

    @Schema(description = "送达渠道：QRCODE 二维码 / LINK 链接；不传按 LINK 记（渠道只用于留痕）")
    private String channel;

    @Schema(description = "有效期（小时），1~720，默认 168（7 天）。到点即失效，对方需要重新发起")
    private Integer expiresInHours;

    /** 有效期小时数（含默认值与上下限），服务层用。 */
    public int resolveExpiresInHours() {
        if (expiresInHours == null) {
            return 168;
        }
        if (expiresInHours < 1) {
            return 1;
        }
        return Math.min(expiresInHours, 720);
    }
}
