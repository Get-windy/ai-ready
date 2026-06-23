package cn.aiedge.erp.finance.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * 所有ERP实体类继承此类，提供通用 auditable 字段
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 创建者ID
     */
    @TableField("created_by")
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新者ID
     */
    @TableField("updated_by")
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 删除标志 (0-正常, 1-已删除)
     */
    @TableLogic
    @TableField("deleted_flag")
    private Integer deletedFlag = 0;

    /**
     * 租户ID (多租户支持)
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;
}
