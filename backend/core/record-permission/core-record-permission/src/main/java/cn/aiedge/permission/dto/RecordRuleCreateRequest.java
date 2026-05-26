package cn.aiedge.permission.dto;

import lombok.Data;

public class RecordRuleCreateRequest {

    private String ruleName;

    private String modelName;

    private String domain;

    private Boolean active;

    private Boolean global;

    private Long groupId;

    private Long userId;

    private Boolean permRead;

    private Boolean permWrite;

    private Boolean permCreate;

    private Boolean permDelete;

    private String description;
}