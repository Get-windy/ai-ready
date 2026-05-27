package cn.aiedge.permission.dto;

import lombok.Data;

@Data
public class FieldPermissionCreateRequest {

    private String modelName;

    private String fieldName;

    private Long groupId;

    private Long userId;

    private Boolean readable;

    private Boolean writable;

    private Boolean required;

    private Boolean hidden;

    private String description;
}