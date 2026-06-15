package cn.aiedge.base.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 认证相关DTO
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public class AuthDTO {

    /**
     * 登录请求
     */
    public record Login(
            @NotBlank(message = "用户名不能为空")
            @JsonProperty("username")
            String username,

            @NotBlank(message = "密码不能为空")
            @JsonProperty("password")
            String password,

            @NotBlank(message = "租户名称不能为空")
            @JsonProperty("tenantName")
            String tenantName
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