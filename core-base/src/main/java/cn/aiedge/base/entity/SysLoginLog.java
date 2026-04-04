package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 登录日志实体
 * 记录用户登录行为，用于安全审计和行为分析
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_login_log")
public class SysLoginLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 登录类型：1-账号密码登录，2-短信验证码登录，3-第三方登录
     */
    private Integer loginType;

    /**
     * 登录结果：0-成功，1-失败
     */
    private Integer loginResult;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 登录IP
     */
    private String loginIp;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 设备类型：PC、Mobile、Tablet
     */
    private String deviceType;

    /**
     * 登录时间
     */
    private LocalDateTime loginTime;

    /**
     * 退出时间
     */
    private LocalDateTime logoutTime;

    /**
     * Token ID（用于关联登出）
     */
    private String tokenId;

    /**
     * 备注
     */
    private String remark;
}