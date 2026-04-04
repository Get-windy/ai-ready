package cn.aiedge.base.dto;

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
            String username,
            
            @NotBlank(message = "密码不能为空")
            String password,
            
            @NotNull(message = "租户ID不能为空")
            Long tenantId
    ) {}

    /**
     * 刷新Token请求
     */
    public record RefreshToken(
            String refreshToken
    ) {}

    /**
     * 修改密码请求
     */
    public record ChangePassword(
            @NotBlank(message = "原密码不能为空")
            String oldPassword,
            
            @NotBlank(message = "新密码不能为空")
            String newPassword,
            
            @NotBlank(message = "确认密码不能为空")
            String confirmPassword
    ) {}

    /**
     * 重置密码请求
     */
    public record ResetPassword(
            @NotBlank(message = "手机号或邮箱不能为空")
            String account,
            
            @NotBlank(message = "验证码不能为空")
            String verifyCode,
            
            @NotBlank(message = "新密码不能为空")
            String newPassword
    ) {}

    /**
     * 发送验证码请求
     */
    public record SendVerifyCode(
            @NotBlank(message = "手机号或邮箱不能为空")
            String account,
            
            @NotNull(message = "验证码类型不能为空")
            Integer type // 1-登录 2-注册 3-重置密码
    ) {}
}