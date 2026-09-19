package cn.aiedge.workflow.entity;

import cn.aiedge.base.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 单据审核规则实体（审核设置 80622）
 * 对应表: sys_audit_rule（V11.398.0 建表）
 *
 * <p>一行 = 一个租户下**一种单据类型**的整组审核规则；规则明细以 JSON 存在
 * {@link #rules}（不做子表：规则条目是「条件枚举 × 审批人集合」的浅结构，
 * 无独立生命周期，符合「表 ≤ 25 列、不做上帝表」的规范口径）。
 *
 * <p>字段继承自 {@code BaseEntity}（id / tenantId / createTime / updateTime /
 * createBy / updateBy / deleted / version），建表脚本已逐列对齐 —— 这是本表能被
 * MyBatis-Plus 的租户插件与逻辑删除、乐观锁插件正确接管的前提。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_audit_rule")
public class SysAuditRuleEntity extends BaseEntity {

    /**
     * 单据类型编码：取值必为 {@link cn.aiedge.workflow.model.AuditRuleCatalog#DOC_TYPES} 的键
     */
    private String docType;

    /**
     * 已启用规则 JSON 数组：
     * {@code [{"condition":"below_cost","approvers":[{"userId":"1","userName":"杨生淮"}]}]}
     */
    private String rules;
}
