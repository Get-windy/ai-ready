package cn.aiedge.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_field_permission")
public class FieldPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String permissionCode;

    private String modelName;

    private String fieldName;

    private Long groupId;

    private String groupName;

    private Long userId;

    private String userName;

    private Boolean readable;

    private Boolean writable;

    private Boolean required;

    private Boolean hidden;

    private String description;

    private Long tenantId;

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
}