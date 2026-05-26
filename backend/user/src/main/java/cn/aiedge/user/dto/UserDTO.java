package cn.aiedge.user.dto;

import cn.aiedge.user.entity.User.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 用户DTO（数据传输对象）
 * 
 * 用于API接口的用户数据传输
 * 
 * @author AI-Ready Team
 * @version 1.0.0
 * @since 2026-04-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度必须在8-32个字符之间")
    private String password;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String fullName;
    
    @Size(max = 20, message = "电话长度不能超过20个字符")
    private String phone;
    
    private UserStatus status;
    
    private String avatarUrl;
    
    private String department;
    
    private String position;
    
    private LocalDateTime lastLoginAt;
    
    private Boolean emailVerified;
    
    private Boolean enabled;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private Set<String> roles;
}