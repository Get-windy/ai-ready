package cn.aiedge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色-单据类型权限实体
 * <p>
 * 控制每个角色可以访问哪些单据类型（如 601=销售出库单、604=销售订单），
 * 以及对应的操作级别（查看/编辑/审核）。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@TableName("sys_role_bill_type")
public class SysRoleBillType {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色ID */
    private Long roleId;

    /** 单据类型代码（如 "601", "604", "504", "801"） */
    private String billType;

    /**
     * 权限级别
     * <ul>
     *   <li>1 = 查看</li>
     *   <li>2 = 编辑</li>
     *   <li>3 = 审核</li>
     * </ul>
     */
    private Integer permissionLevel;

    /** 创建人 */
    private Long createdBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
