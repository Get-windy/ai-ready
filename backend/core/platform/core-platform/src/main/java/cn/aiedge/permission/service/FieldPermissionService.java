package cn.aiedge.permission.service;

import cn.aiedge.permission.dto.*;
import cn.aiedge.permission.entity.FieldPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface FieldPermissionService {

    FieldPermission createPermission(FieldPermissionCreateRequest request);

    FieldPermission updatePermission(Long id, FieldPermissionCreateRequest request);

    FieldPermission getPermissionById(Long id);

    List<FieldPermission> getPermissionsByModel(String modelName);

    List<FieldPermission> getPermissionsByModelAndUser(String modelName, Long userId);

    List<FieldPermission> getPermissionsByModelAndGroup(String modelName, Long groupId);

    Page<FieldPermission> listPermissions(Integer page, Integer size, String modelName, String fieldName);

    void deletePermission(Long id);

    Map<String, FieldPermissionResult> getFieldPermissions(String modelName, PermissionContext context);

    FieldPermissionResult getSingleFieldPermission(String modelName, String fieldName, PermissionContext context);

    boolean isFieldReadable(String modelName, String fieldName, PermissionContext context);

    boolean isFieldWritable(String modelName, String fieldName, PermissionContext context);

    boolean isFieldRequired(String modelName, String fieldName, PermissionContext context);

    boolean isFieldHidden(String modelName, String fieldName, PermissionContext context);

    Map<String, Object> filterRecordFields(String modelName, Map<String, Object> recordData, PermissionContext context);

    void applyFieldPermissions(String modelName, Map<String, Object> recordData, PermissionContext context, boolean forWrite);
}