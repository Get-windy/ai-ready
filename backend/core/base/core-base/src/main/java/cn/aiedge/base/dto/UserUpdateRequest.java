package cn.aiedge.base.dto;

import lombok.Data;

/**
 * 更新用户请求
 */
@Data
public class UserUpdateRequest {
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer gender;
    private Long deptId;
    private Long postId;
}
