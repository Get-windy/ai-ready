package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据权限范围规则
 * <p>
 * 定义角色对特定表的行级数据可见范围，支持部门树选择和自定义SQL两种模式。
 * 当角色的 dataScope = 4 (CUSTOM) 时，查询此表获取精细化的可见范围定义。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_scope")
public class SysDataScope extends BaseEntity {

    /** 角色ID */
    private Long roleId;

    /** 规则类型: ALL/DEPT/DEPT_AND_CHILD/SELF/CUSTOM_SQL */
    private String ruleType;

    /** 目标表名（为空时对全部表生效） */
    private String targetTable;

    /** 目标字段名（默认 dept_id） */
    private String targetField;

    /** 可见部门ID列表（JSON数组，ruleType=DEPT/DEPT_AND_CHILD 时使用） */
    private String deptIds;

    /** 自定义SQL条件（ruleType=CUSTOM_SQL 时使用，如 "dept_id IN (1,2,3) AND status=1"） */
    private String customSql;

    /** 备注 */
    private String remark;

    /** 状态 0-禁用 1-启用 */
    private Integer status;
}
