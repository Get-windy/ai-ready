package cn.aiedge.permission.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecordRuleDTO {

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

    private Boolean permRead;

    private Boolean permWrite;

    private Boolean permCreate;

    private Boolean permDelete;

    private String description;

    private LocalDateTime createTime;
}