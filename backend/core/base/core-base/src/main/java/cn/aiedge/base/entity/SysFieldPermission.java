package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字段级权限
 * <p>
 * 控制角色对特定表字段的可见性和脱敏规则。
 * 可在前端页面动态配置，后端序列化时自动应用。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_field_permission")
public class SysFieldPermission extends BaseEntity {

    /** 角色ID */
    private Long roleId;

    /** 目标表名 */
    private String targetTable;

    /** 目标字段名 */
    private String targetField;

    /** 是否可见 1-可见 0-隐藏 */
    private Integer visible;

    /** 脱敏类型: NONE/PHONE/EMAIL/ID_CARD/BANK_CARD/CUSTOM */
    private String maskType;

    /** 脱敏填充字符（默认 *） */
    private String maskChar;

    /** 保留前缀长度 */
    private Integer maskPrefixLen;

    /** 保留后缀长度 */
    private Integer maskSuffixLen;

    /** 状态 0-禁用 1-启用 */
    private Integer status;
}
