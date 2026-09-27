package cn.aiedge.base.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 认证相关DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class AuthDTO {

    /**
     * 登录请求
     * <p>
     * 不再要求用户输入企业名称：用户名全局唯一，凭用户名+密码即可确认身份；
     * 企业由「用户名所属的企业列表」推导 —— 只有一个则直接进入，多个则返回候选让用户选。
     * </p>
     */
    public record Login(
            @NotBlank(message = "用户名不能为空")
            @JsonProperty("username")
            String username,

            @NotBlank(message = "密码不能为空")
            @JsonProperty("password")
            String password,

            @JsonProperty("captcha")
            String captcha,

            @JsonProperty("captchaKey")
            String captchaKey
    ) {}

    /**
     * 发送登录短信验证码
     */
    public record SendSmsCode(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
            @JsonProperty("phone")
            String phone
    ) {}

    /**
     * 手机号验证码登录
     */
    public record LoginBySms(
            @NotBlank(message = "手机号不能为空")
            @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
            @JsonProperty("phone")
            String phone,

            @NotBlank(message = "验证码不能为空")
            @JsonProperty("smsCode")
            String smsCode
    ) {}

    /**
     * 三方登录换取 Token（回调后前端用一次性票据兑换，避免 Token 出现在 URL 里）
     */
    public record SocialExchange(
            @NotBlank(message = "票据不能为空")
            @JsonProperty("ticket")
            String ticket
    ) {}

    /**
     * 选择企业（多企业用户登录第二步）
     */
    public record SelectTenant(
            @NotBlank(message = "登录票据不能为空")
            @JsonProperty("selectToken")
            String selectToken,

            @NotNull(message = "请选择企业")
            @JsonProperty("tenantId")
            Long tenantId
    ) {}

    /**
     * 切换企业（已登录用户）
     */
    public record SwitchTenant(
            @NotNull(message = "请选择企业")
            @JsonProperty("tenantId")
            Long tenantId
    ) {}

    /**
     * 刷新Token请求
     */
    public record RefreshToken(
            @JsonProperty("refreshToken")
            String refreshToken
    ) {}

    /**
     * 修改密码请求
     */
    public record ChangePassword(
            @NotBlank(message = "原密码不能为空")
            @JsonProperty("oldPassword")
            String oldPassword,

            @NotBlank(message = "新密码不能为空")
            @JsonProperty("newPassword")
            String newPassword,

            @NotBlank(message = "确认密码不能为空")
            @JsonProperty("confirmPassword")
            String confirmPassword
    ) {}

    /**
     * 重置密码请求
     */
    public record ResetPassword(
            @NotBlank(message = "手机号或邮箱不能为空")
            @JsonProperty("account")
            String account,

            @NotBlank(message = "验证码不能为空")
            @JsonProperty("verifyCode")
            String verifyCode,

            @NotBlank(message = "新密码不能为空")
            @JsonProperty("newPassword")
            String newPassword
    ) {}

    /**
     * 发送验证码请求
     */
    public record SendVerifyCode(
            @NotBlank(message = "手机号或邮箱不能为空")
            @JsonProperty("account")
            String account,

            @NotNull(message = "验证码类型不能为空")
            @JsonProperty("type")
            Integer type // 1-登录 2-注册 3-重置密码
    ) {}
}