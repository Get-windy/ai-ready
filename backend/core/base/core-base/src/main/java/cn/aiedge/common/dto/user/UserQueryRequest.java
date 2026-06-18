package cn.aiedge.common.dto.user;

import lombok.Data;

/**
 * 用户查询请求DTO
 */
@Data
public class UserQueryRequest {

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 真实姓名（模糊查询）
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
