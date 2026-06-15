package cn.aiedge.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建用户请求
 */
@Data
public class UserCreateRequest {
    private Long tenantId;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度3-50字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度6-100字符")
    private String password;

    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer gender;
    private Integer userType;
    private Long deptId;
    private Long postId;
}
