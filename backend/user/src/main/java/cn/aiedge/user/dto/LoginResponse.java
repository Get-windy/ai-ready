package cn.aiedge.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应DTO
 * 
 * 用于用户登录接口的响应数据
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    
    private String accessToken;
    
    private String refreshToken;
    
    private String tokenType = "Bearer";
    
    private Long expiresIn;
    
    private UserDTO user;
    
    private String message;
}