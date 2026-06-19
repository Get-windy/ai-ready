package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 职责分离规则（Segregation of Duties）
 * <p>
 * 定义互斥角色关系。同一用户不能同时拥有互斥的角色组合。
 * 例如：不能同时拥有"采购员"和"审批员"角色。
 * 在分配角色时自动校验，防止权限集中导致的舞弊风险。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_sod_rule")
public class SysSodRule extends BaseEntity {

    /** 规则名称 */
    private String ruleName;

    /** 规则描述 */
    private String description;

    /** 互斥角色ID列表（JSON数组，如 "[1,5,8]"） */
    private String conflictRoleIds;

    /** 状态 0-禁用 1-启用 */
    private Integer status;
}
