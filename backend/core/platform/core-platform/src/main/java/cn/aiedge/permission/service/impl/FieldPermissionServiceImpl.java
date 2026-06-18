package cn.aiedge.permission.service.impl;

import cn.aiedge.permission.dto.*;
import cn.aiedge.permission.entity.FieldPermission;
import cn.aiedge.permission.mapper.FieldPermissionMapper;
import cn.aiedge.permission.service.FieldPermissionService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FieldPermissionServiceImpl implements FieldPermissionService {

    private final FieldPermissionMapper permissionMapper;

    @Override
    @Transactional
    public FieldPermission createPermission(FieldPermissionCreateRequest request) {
        FieldPermission permission = new FieldPermission();
        permission.setPermissionCode("FP" + IdUtil.fastSimpleUUID().substring(0, 8));
        permission.setModelName(request.getModelName());
        permission.setFieldName(request.getFieldName());
        permission.setGroupId(request.getGroupId());
        permission.setUserId(request.getUserId());
        permission.setReadable(request.getReadable() != null ? request.getReadable() : true);
        permission.setWritable(request.getWritable() != null ? request.getWritable() : true);
        permission.setRequired(request.getRequired() != null ? request.getRequired() : false);
        permission.setHidden(request.getHidden() != null ? request.getHidden() : false);
        permission.setDescription(request.getDescription());
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    @Transactional
    public FieldPermission updatePermission(Long id, FieldPermissionCreateRequest request) {
        FieldPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new RuntimeException("权限不存在: " + id);
        }
        permission.setModelName(request.getModelName());
        permission.setFieldName(request.getFieldName());
        permission.setGroupId(request.getGroupId());
        permission.setUserId(request.getUserId());
        permission.setReadable(request.getReadable());
        permission.setWritable(request.getWritable());
        permission.setRequired(request.getRequired());
        permission.setHidden(request.getHidden());
        permission.setDescription(request.getDescription());
        permissionMapper.updateById(permission);
        return permission;
    }

    @Override
    public FieldPermission getPermissionById(Long id) {
        return permissionMapper.selectById(id);
    }

    @Override
    public List<FieldPermission> getPermissionsByModel(String modelName) {
        return permissionMapper.selectByModel(modelName);
    }

    @Override
    public List<FieldPermission> getPermissionsByModelAndUser(String modelName, Long userId) {
        return permissionMapper.selectByModelAndUser(modelName, userId);
    }

    @Override
    public List<FieldPermission> getPermissionsByModelAndGroup(String modelName, Long groupId) {
        return permissionMapper.selectByModelAndGroup(modelName, groupId);
    }

    @Override
    public Page<FieldPermission> listPermissions(Integer page, Integer size, String modelName, String fieldName) {
        Page<FieldPermission> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<FieldPermission> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(modelName)) {
            wrapper.eq(FieldPermission::getModelName, modelName);
        }
        if (StrUtil.isNotBlank(fieldName)) {
            wrapper.eq(FieldPermission::getFieldName, fieldName);
        }
        wrapper.orderByAsc(FieldPermission::getFieldName);
        return permissionMapper.selectPage(pageObj, wrapper);
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        permissionMapper.deleteById(id);
    }

    @Override
    public Map<String, FieldPermissionResult> getFieldPermissions(String modelName, PermissionContext context) {
        List<FieldPermission> allPermissions = new ArrayList<>();

        if (context.getUserId() != null) {
            List<FieldPermission> userPermissions = permissionMapper.selectByModelAndUser(modelName, context.getUserId());
            allPermissions.addAll(userPermissions);
        }

        if (context.getGroupIds() != null && !context.getGroupIds().isEmpty()) {
            for (Long groupId : context.getGroupIds()) {
                List<FieldPermission> groupPermissions = permissionMapper.selectByModelAndGroup(modelName, groupId);
                allPermissions.addAll(groupPermissions);
            }
        }

        Map<String, FieldPermissionResult> result = new HashMap<>();
        Map<String, List<FieldPermission>> fieldPermissionMap = allPermissions.stream()
                .collect(Collectors.groupingBy(FieldPermission::getFieldName));

        for (Map.Entry<String, List<FieldPermission>> entry : fieldPermissionMap.entrySet()) {
            String fieldName = entry.getKey();
            List<FieldPermission> permissions = entry.getValue();

            FieldPermissionResult fieldResult = new FieldPermissionResult();
            fieldResult.setFieldName(fieldName);

            boolean readable = permissions.stream().anyMatch(p -> p.getReadable());
            boolean writable = permissions.stream().anyMatch(p -> p.getWritable());
            boolean required = permissions.stream().anyMatch(p -> p.getRequired());
            boolean hidden = permissions.stream().anyMatch(p -> p.getHidden());

            fieldResult.setReadable(readable);
            fieldResult.setWritable(writable);
            fieldResult.setRequired(required);
            fieldResult.setHidden(hidden);

            List<String> readableGroups = permissions.stream()
                    .filter(p -> p.getReadable() && p.getGroupName() != null)
                    .map(FieldPermission::getGroupName)
                    .collect(Collectors.toList());
            fieldResult.setReadableGroups(readableGroups);

            List<String> writableGroups = permissions.stream()
                    .filter(p -> p.getWritable() && p.getGroupName() != null)
                    .map(FieldPermission::getGroupName)
                    .collect(Collectors.toList());
            fieldResult.setWritableGroups(writableGroups);

            result.put(fieldName, fieldResult);
        }

        return result;
    }

    @Override
    public FieldPermissionResult getSingleFieldPermission(String modelName, String fieldName, PermissionContext context) {
        Map<String, FieldPermissionResult> allPermissions = getFieldPermissions(modelName, context);
        return allPermissions.getOrDefault(fieldName, createDefaultPermission(fieldName));
    }

    @Override
    public boolean isFieldReadable(String modelName, String fieldName, PermissionContext context) {
        FieldPermissionResult result = getSingleFieldPermission(modelName, fieldName, context);
        return result.getReadable();
    }

    @Override
    public boolean isFieldWritable(String modelName, String fieldName, PermissionContext context) {
        FieldPermissionResult result = getSingleFieldPermission(modelName, fieldName, context);
        return result.getWritable();
    }

    @Override
    public boolean isFieldRequired(String modelName, String fieldName, PermissionContext context) {
        FieldPermissionResult result = getSingleFieldPermission(modelName, fieldName, context);
        return result.getRequired();
    }

    @Override
    public boolean isFieldHidden(String modelName, String fieldName, PermissionContext context) {
        FieldPermissionResult result = getSingleFieldPermission(modelName, fieldName, context);
        return result.getHidden();
    }

    @Override
    public Map<String, Object> filterRecordFields(String modelName, Map<String, Object> recordData, PermissionContext context) {
        Map<String, FieldPermissionResult> permissions = getFieldPermissions(modelName, context);

        Map<String, Object> filteredData = new HashMap<>();
        for (Map.Entry<String, Object> entry : recordData.entrySet()) {
            String fieldName = entry.getKey();
            FieldPermissionResult permission = permissions.get(fieldName);

            if (permission == null) {
                filteredData.put(fieldName, entry.getValue());
            } else if (!permission.getHidden()) {
                filteredData.put(fieldName, entry.getValue());
            }
        }

        return filteredData;
    }

    @Override
    public void applyFieldPermissions(String modelName, Map<String, Object> recordData, PermissionContext context, boolean forWrite) {
        Map<String, FieldPermissionResult> permissions = getFieldPermissions(modelName, context);

        List<String> fieldsToRemove = new ArrayList<>();
        for (Map.Entry<String, Object> entry : recordData.entrySet()) {
            String fieldName = entry.getKey();
            FieldPermissionResult permission = permissions.get(fieldName);

            if (permission != null) {
                if (permission.getHidden()) {
                    fieldsToRemove.add(fieldName);
                } else if (forWrite && !permission.getWritable()) {
                    fieldsToRemove.add(fieldName);
                }
            }
        }

        for (String field : fieldsToRemove) {
            recordData.remove(field);
        }
    }

    private FieldPermissionResult createDefaultPermission(String fieldName) {
        FieldPermissionResult result = new FieldPermissionResult();
        result.setFieldName(fieldName);
        result.setReadable(true);
        result.setWritable(true);
        result.setRequired(false);
        result.setHidden(false);
        return result;
    }
}