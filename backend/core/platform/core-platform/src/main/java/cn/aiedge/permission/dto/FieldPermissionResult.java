package cn.aiedge.permission.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FieldPermissionResult {

    private String fieldName;

    private Boolean readable;

    private Boolean writable;

    private Boolean required;

    private Boolean hidden;

    private List<String> readableGroups;

    private List<String> writableGroups;
}