package cn.aiedge.customfield.service.impl;

import cn.aiedge.customfield.dto.*;
import cn.aiedge.customfield.entity.CustomField;
import cn.aiedge.customfield.entity.CustomFieldGroup;
import cn.aiedge.customfield.enums.FieldType;
import cn.aiedge.customfield.mapper.CustomFieldGroupMapper;
import cn.aiedge.customfield.mapper.CustomFieldMapper;
import cn.aiedge.customfield.service.CustomFieldService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomFieldServiceImpl implements CustomFieldService {

    private final CustomFieldMapper fieldMapper;
    private final CustomFieldGroupMapper groupMapper;

    @Override
    @Transactional
    public CustomField createField(CustomFieldCreateRequest request) {
        if (!validateFieldName(request.getModelName(), request.getFieldName())) {
            throw new RuntimeException("字段名称已存在: " + request.getFieldName());
        }

        CustomField field = new CustomField();
        field.setFieldCode("CF" + IdUtil.fastSimpleUUID().substring(0, 8));
        field.setModelName(request.getModelName());
        field.setFieldName(request.getFieldName());
        field.setFieldType(request.getFieldType());
        field.setFieldLabel(request.getFieldLabel());
        field.setRequired(request.getRequired() != null ? request.getRequired() : false);
        field.setReadonly(request.getReadonly() != null ? request.getReadonly() : false);
        field.setSearchable(request.getSearchable() != null ? request.getSearchable() : false);
        field.setSortable(request.getSortable() != null ? request.getSortable() : false);
        field.setDefaultValue(request.getDefaultValue());
        field.setSelectionValues(request.getSelectionValues());
        field.setValidationRule(request.getValidationRule());
        field.setHelpText(request.getHelpText());
        field.setPlaceholder(request.getPlaceholder());
        field.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        field.setGroupCode(request.getGroupCode());
        field.setActive(true);
        fieldMapper.insert(field);
        return field;
    }

    @Override
    @Transactional
    public CustomField updateField(Long id, CustomFieldCreateRequest request) {
        CustomField field = fieldMapper.selectById(id);
        if (field == null) {
            throw new RuntimeException("字段不存在: " + id);
        }

        if (!field.getFieldName().equals(request.getFieldName())) {
            if (!validateFieldName(request.getModelName(), request.getFieldName())) {
                throw new RuntimeException("字段名称已存在: " + request.getFieldName());
            }
        }

        field.setModelName(request.getModelName());
        field.setFieldName(request.getFieldName());
        field.setFieldType(request.getFieldType());
        field.setFieldLabel(request.getFieldLabel());
        field.setRequired(request.getRequired());
        field.setReadonly(request.getReadonly());
        field.setSearchable(request.getSearchable());
        field.setSortable(request.getSortable());
        field.setDefaultValue(request.getDefaultValue());
        field.setSelectionValues(request.getSelectionValues());
        field.setValidationRule(request.getValidationRule());
        field.setHelpText(request.getHelpText());
        field.setPlaceholder(request.getPlaceholder());
        field.setSortOrder(request.getSortOrder());
        field.setGroupCode(request.getGroupCode());
        fieldMapper.updateById(field);
        return field;
    }

    @Override
    public CustomField getFieldById(Long id) {
        return fieldMapper.selectById(id);
    }

    @Override
    public List<CustomField> getFieldsByModel(String modelName) {
        return fieldMapper.selectByModelName(modelName);
    }

    @Override
    public List<CustomField> getFieldsByModelAndGroup(String modelName, String groupCode) {
        return fieldMapper.selectByModelAndGroup(modelName, groupCode);
    }

    @Override
    public Page<CustomField> listFields(Integer page, Integer size, String modelName, String fieldType) {
        Page<CustomField> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<CustomField> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(modelName)) {
            wrapper.eq(CustomField::getModelName, modelName);
        }
        if (StrUtil.isNotBlank(fieldType)) {
            wrapper.eq(CustomField::getFieldType, fieldType);
        }
        wrapper.orderByAsc(CustomField::getSortOrder);
        return fieldMapper.selectPage(pageObj, wrapper);
    }

    @Override
    @Transactional
    public void deleteField(Long id) {
        fieldMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void activateField(Long id) {
        CustomField field = fieldMapper.selectById(id);
        if (field != null) {
            field.setActive(true);
            fieldMapper.updateById(field);
        }
    }

    @Override
    @Transactional
    public void deactivateField(Long id) {
        CustomField field = fieldMapper.selectById(id);
        if (field != null) {
            field.setActive(false);
            fieldMapper.updateById(field);
        }
    }

    @Override
    public List<CustomFieldGroup> getGroupsByModel(String modelName) {
        return groupMapper.selectByModelName(modelName);
    }

    @Override
    @Transactional
    public CustomFieldGroup createGroup(String modelName, String groupName, String description) {
        CustomFieldGroup group = new CustomFieldGroup();
        group.setGroupCode("GRP" + IdUtil.fastSimpleUUID().substring(0, 8));
        group.setModelName(modelName);
        group.setGroupName(groupName);
        group.setDescription(description);
        group.setSortOrder(0);
        group.setActive(true);
        groupMapper.insert(group);
        return group;
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        groupMapper.deleteById(id);
    }

    @Override
    public boolean validateFieldName(String modelName, String fieldName) {
        return fieldMapper.countByModelAndFieldName(modelName, fieldName) == 0;
    }

    @Override
    public List<CustomField> getSearchableFields(String modelName) {
        return fieldMapper.selectSearchableFields(modelName);
    }
}