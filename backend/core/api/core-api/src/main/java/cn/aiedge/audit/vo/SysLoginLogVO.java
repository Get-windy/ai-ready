package cn.aiedge.audit.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 登录日志视图对象（设置 → 账套操作 → 操作日志 → 登录日志 Tab）
 *
 * <p>与 {@link SysOperLogVO} 同因：对外只暴露台账需要的列，并补「姓名」列
 * （{@code sys_login_log} 只有 username，真实姓名来自 {@code sys_user.real_name}）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Accessors(chain = true)
public class SysLoginLogVO {

    /** 日志ID（雪花ID，前端按字符串处理） */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 操作员（登录账号） */
    private String username;

    /** 姓名（来自 sys_user.real_name，空则回退 nickname；两侧都空时为 null） */
    private String realName;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 登录类型：1-账号密码登录，2-短信验证码登录，3-第三方登录（值为空的按「未知」处理） */
    private Integer loginType;

    /** 登录结果：0-成功，1-失败 */
    private Integer loginResult;

    /** 失败原因（登录失败时） */
    private String failReason;

    /** 登录IP */
    private String loginIp;

    /** 登录地点 */
    private String loginLocation;

    /** 浏览器类型 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 设备类型：PC / Mobile / Tablet / Unknown */
    private String deviceType;

    /** 退出时间（未登出为 null） */
    private LocalDateTime logoutTime;

    /** 备注 */
    private String remark;
}
