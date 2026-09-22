package cn.aiedge.agreement.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请（唯一送达）的下发形态。
 *
 * <h3>⚠️ 明文 token 只在**创建那一次**返回</h3>
 * 库中只存哈希（见 {@code AgreementInviteToken} 的注释），因此
 * {@link #token} 与 {@link #shortLink} **只在发起邀请的响应里非空**；
 * 之后任何查询（列表 / 详情 / 撤回响应）都只给 {@link #tokenHint}（明文前 8 位）与
 * {@link #inviteCode}（人读短码）。这样即使前端日志、浏览器历史泄露了列表响应，
 * 也换不出可用的邀请链接。
 */
@Data
public class AgreementInviteVO {

    private Long id;

    private Long agreementId;

    private String agreementNo;

    private String agreementTitle;

    /** 绑定的目标文稿版本（token 五绑定之③） */
    private Long versionId;

    private Integer versionNo;

    /** 目标主体 + 目标租户（五绑定之① ②） */
    private Long targetPartyId;
    private String targetPartyName;
    private Long targetTenantId;
    private String targetTenantName;
    private String targetSide;
    private String targetSideLabel;

    /** 0=待领取 / 1=已领取 / 2=已撤回 / 3=已过期 */
    private Integer status;
    private String statusLabel;

    private String channel;
    private String channelLabel;

    /** 五绑定之④：有效期截止时刻 */
    private LocalDateTime expiresAt;

    /** 五绑定之⑤：一次性 —— 领取人/时间/渠道 */
    private Long acceptedBy;
    private LocalDateTime acceptedAt;
    private String acceptChannel;
    private Long acceptedPartyId;

    /** 查看 / 领取留痕：谁、何时、通过哪个渠道 */
    private Integer viewCount;
    private Long firstViewedBy;
    private LocalDateTime firstViewedAt;
    private String firstViewChannel;

    private Long revokeBy;
    private LocalDateTime revokeAt;
    private String revokeReason;

    private Long createdBy;
    private LocalDateTime createdAt;

    /** 明文 token —— **只在下发（创建）那一次返回**；后续查询为空 */
    private String token;

    /** 明文 token 前 8 位，供运维/客诉在库里对上记录 */
    private String tokenHint;

    /** 人读邀请码（可口述、可人工输入） */
    private String inviteCode;

    /** 可供前端渲染二维码 / 点开的短链（站内相对路径）；**只在下发那一次返回** */
    private String shortLink;

    /** 使用说明（中文白话，前端直接显示） */
    private String usageNote;
}
