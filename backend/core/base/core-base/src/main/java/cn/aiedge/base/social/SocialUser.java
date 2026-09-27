package cn.aiedge.base.social;

/**
 * 三方平台返回的用户身份（各平台字段口径统一后的结果）。
 *
 * @param platform   平台标识：{@code dingtalk} / {@code wecom} / {@code feishu}
 * @param corpId     三方侧企业标识（服务商模式下每个客户企业不同；钉钉扫码登录可能为空）
 * @param openId     平台内用户标识（同一应用内唯一，**参与唯一约束，必填**）
 * @param unionId    跨应用唯一标识（钉钉 unionId / 微信 unionid / 飞书 union_id）
 * @param corpUserId 企业内成员 ID（企微 userid / 钉钉 userid）
 * @param nickname   昵称
 * @param avatar     头像地址
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
public record SocialUser(
        String platform,
        String corpId,
        String openId,
        String unionId,
        String corpUserId,
        String nickname,
        String avatar
) {
}
