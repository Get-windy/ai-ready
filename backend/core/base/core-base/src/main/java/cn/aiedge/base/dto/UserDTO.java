package cn.aiedge.base.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户数据传输对象
 * 使用 Java 17 Record 简化代码
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public sealed interface UserDTO permits UserDTO.Create, UserDTO.Update, UserDTO.Query, UserDTO.Login {

    /**
     * 创建用户DTO
     */
    record Create(
            Long tenantId,
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 50, message = "用户名长度3-50字符")
            String username,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 100, message = "密码长度6-100字符")
            String password,
            String nickname,
            String email,
            String phone,
            String avatar,
            Integer gender,
            Integer userType,
            Long deptId,
            Long postId
    ) implements UserDTO {}

    /**
     * 更新用户DTO
     */
    record Update(
            Long id,
            String nickname,
            String email,
            String phone,
            String avatar,
            Integer gender,
            Long deptId,
            Long postId
    ) implements UserDTO {}

    /**
     * 查询用户DTO
     */
    record Query(
            Long tenantId,
            String username,
            Integer status,
            Long deptId,
            Integer pageNum,
            Integer pageSize
    ) implements UserDTO {}

    /**
     * 登录DTO
     */
    record Login(
            @NotBlank(message = "用户名不能为空")
            String username,
            @NotBlank(message = "密码不能为空")
            String password,
            Long tenantId
    ) implements UserDTO {}
}