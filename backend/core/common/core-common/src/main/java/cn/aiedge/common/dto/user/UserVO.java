package cn.aiedge.common.dto.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应VO
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String realName;

    private String nickname;

    private String avatar;

    private String email;

    private String phone;

    private Integer gender;

    private Long deptId;

    private String deptName;

    private Long postId;

    private Integer status;

    private Boolean isSuperAdmin;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 用户角色列表
     */
    private List<RoleVO> roles;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;
}
