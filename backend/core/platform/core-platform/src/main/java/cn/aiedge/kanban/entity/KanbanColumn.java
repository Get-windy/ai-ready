package cn.aiedge.kanban.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_kanban_column")
public class KanbanColumn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String columnCode;

    private String modelName;

    private String groupField;

    private String groupValue;

    private String columnName;

    private String color;

    private Integer sortOrder;

    private Boolean active;

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