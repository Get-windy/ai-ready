package cn.aiedge.erp.party.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户区域（资料 → 往来单位 → 客户 → 区域管理子标签）
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@TableName("erp_customer_region")
public class CustomerRegion {

    /** 主键：雪花 ID（与资料模块其它档案一致，避免依赖数据库自增序列） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long tenantId;

    /** 区域编号 */
    private String regionCode;

    /** 区域名称 */
    private String regionName;

    /** 上级区域（根节点为 0） */
    private Long parentId;

    /** 层级 */
    private Integer regionLevel;

    private Integer sortOrder;

    private Integer status;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 子区域（树形返回时装配，非表字段） */
    @TableField(exist = false)
    private List<CustomerRegion> children;
}
