package cn.aiedge.permission.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PermissionContext {

    private Long userId;

    private String userName;

    private List<Long> groupIds;

    private List<String> groupNames;

    private Long tenantId;

    private String modelName;

    private String operation;

    private Map<String, Object> recordData;
}