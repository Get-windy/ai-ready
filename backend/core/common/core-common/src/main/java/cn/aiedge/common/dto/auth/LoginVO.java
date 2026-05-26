package cn.aiedge.common.dto.auth;

import lombok.Data;

import java.util.List;

/**
 * 登录响应VO
 */
@Data
public class LoginVO {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息
     */
    private UserInfo userInfo;

    /**
     * 用户简要信息
     */
    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String realName;
        private String nickname;
        private String avatar;
        private String email;
        private String phone;
        private Long deptId;
        private String deptName;
        private List<String> roles;
        private List<String> permissions;
    }
}
