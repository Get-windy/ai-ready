package cn.aiedge.permission.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FieldPermissionDTO {

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

    private LocalDateTime createTime;
}