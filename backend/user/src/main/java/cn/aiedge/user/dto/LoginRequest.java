package cn.aiedge.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求DTO
 * 
 * 用于用户登录接口的请求参数
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "用户名/邮箱不能为空")
    private String usernameOrEmail;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private Boolean rememberMe = false;
}