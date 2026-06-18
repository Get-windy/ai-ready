package cn.aiedge.kanban.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_kanban_card")
public class KanbanCard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cardCode;

    private String modelName;

    private Long recordId;

    private Long columnId;

    private String title;

    private String description;

    private String priority;

    private Long assigneeId;

    private String assigneeName;

    private LocalDateTime dueDate;

    private String tags;

    private Integer sortOrder;

    private String status;

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