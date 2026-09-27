package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户三方账号绑定（钉钉 / 企业微信 / 飞书）
 * <p>
 * 归属是「用户」而非「租户」：一个用户可能属于多个企业，绑定关系跟着人走，
 * 因此本表没有 {@code tenant_id} 列，已在 {@code MyBatisPlusConfig} 的租户插件忽略清单中登记。
 * </p>
 *
 * @author AI-Ready Team
 * @since 0.3.28
 */
@Data
@Accessors(chain = true)
@TableName("sys_user_social_binding")
public class SysUserSocialBinding {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 系统用户ID */
    private Long userId;

    /** 三方平台：{@code dingtalk} / {@code wecom} / {@code feishu} */
    private String platform;

    /**
     * 三方侧企业标识。
     * <p>路线 B（服务商模式）下每个客户企业各不相同，是「同一平台不同应用」的区分键。</p>
     */
    private String corpId;

    /** 平台内用户标识（同一应用内唯一） */
    private String openId;

    /** 跨应用唯一标识（钉钉 unionId / 微信 unionid / 飞书 union_id） */
    private String unionId;

    /** 企业内成员 ID（企微 userid / 钉钉 userid），供组织相关能力使用 */
    private String corpUserId;

    private String nickname;

    private String avatar;

    /** 绑定时间 */
    private LocalDateTime bindTime;

    /** 上次用该三方账号登录的时间 */
    private LocalDateTime lastLoginTime;

    /** 逻辑删除：0-正常 1-已解绑 */
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
