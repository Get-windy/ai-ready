package cn.aiedge.permission.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_record_rule")
public class RecordRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleCode;

    private String ruleName;

    private String modelName;

    private String domain;

    private Boolean active;

    private Boolean global;

    private Long groupId;

    private String groupName;

    private Long userId;

    private String userName;

    private Integer permRead;

    private Integer permWrite;

    private Integer permCreate;

    private Integer permDelete;

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