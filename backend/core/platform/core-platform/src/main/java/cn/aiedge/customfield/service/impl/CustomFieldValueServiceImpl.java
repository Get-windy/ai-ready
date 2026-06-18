package cn.aiedge.customfield.service.impl;

import cn.aiedge.customfield.dto.*;
import cn.aiedge.customfield.entity.CustomField;
import cn.aiedge.customfield.entity.CustomFieldValue;
import cn.aiedge.customfield.enums.FieldType;
import cn.aiedge.customfield.mapper.CustomFieldMapper;
import cn.aiedge.customfield.mapper.CustomFieldValueMapper;
import cn.aiedge.customfield.service.CustomFieldValueService;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomFieldValueServiceImpl implements CustomFieldValueService {

    private final CustomFieldMapper fieldMapper;
    private final CustomFieldValueMapper valueMapper;

    @Override
    @Transactional
    public void saveFieldValue(String modelName, Long recordId, Long fieldId, Object value) {
        CustomField field = fieldMapper.selectById(fieldId);
        if (field == null) {
            throw new RuntimeException("字段不存在: " + fieldId);
        }

        CustomFieldValue existing = valueMapper.selectByFieldAndRecord(fieldId, recordId);
        if (existing == null) {
            existing = new CustomFieldValue();
            existing.setFieldId(fieldId);
            existing.setModelName(modelName);
            existing.setRecordId(recordId);
        }

        setValueByType(existing, field.getFieldType(), value);
        
        if (existing.getId() == null) {
            valueMapper.insert(existing);
        } else {
            valueMapper.updateById(existing);
        }
    }

    @Override
    @Transactional
    public void saveFieldValues(String modelName, Long recordId, Map<Long, Object> fieldValues) {
        for (Map.Entry<Long, Object> entry : fieldValues.entrySet()) {
            saveFieldValue(modelName, recordId, entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Transactional
    public void saveFieldValuesByName(String modelName, Long recordId, Map<String, Object> fieldValues) {
        List<CustomField> fields = fieldMapper.selectByModelName(modelName);
        Map<String, CustomField> fieldMap = new HashMap<>();
        for (CustomField field : fields) {
            fieldMap.put(field.getFieldName(), field);
        }

        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            CustomField field = fieldMap.get(entry.getKey());
            if (field != null) {
                saveFieldValue(modelName, recordId, field.getId(), entry.getValue());
            }
        }
    }

    @Override
    public Object getFieldValue(String modelName, Long recordId, Long fieldId) {
        CustomFieldValue value = valueMapper.selectByFieldAndRecord(fieldId, recordId);
        if (value == null) {
            return null;
        }

        CustomField field = fieldMapper.selectById(fieldId);
        if (field == null) {
            return null;
        }

        return getValueByType(value, field.getFieldType());
    }

    @Override
    public Object getFieldValueByName(String modelName, Long recordId, String fieldName) {
        List<CustomField> fields = fieldMapper.selectByModelName(modelName);
        CustomField field = fields.stream()
                .filter(f -> f.getFieldName().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (field == null) {
            return null;
        }

        return getFieldValue(modelName, recordId, field.getId());
    }

    @Override
    public Map<String, Object> getAllFieldValues(String modelName, Long recordId) {
        List<CustomField> fields = fieldMapper.selectByModelName(modelName);
        List<CustomFieldValue> values = valueMapper.selectByRecord(modelName, recordId);

        Map<Long, CustomFieldValue> valueMap = new HashMap<>();
        for (CustomFieldValue value : values) {
            valueMap.put(value.getFieldId(), value);
        }

        Map<String, Object> result = new HashMap<>();
        for (CustomField field : fields) {
            CustomFieldValue value = valueMap.get(field.getId());
            if (value != null) {
                result.put(field.getFieldName(), getValueByType(value, field.getFieldType()));
            } else {
                result.put(field.getFieldName(), field.getDefaultValue());
            }
        }

        return result;
    }

    @Override
    public List<CustomFieldValue> getFieldValuesByRecord(String modelName, Long recordId) {
        return valueMapper.selectByRecord(modelName, recordId);
    }

    @Override
    @Transactional
    public void deleteFieldValues(String modelName, Long recordId) {
        List<CustomFieldValue> values = valueMapper.selectByRecord(modelName, recordId);
        for (CustomFieldValue value : values) {
            valueMapper.deleteById(value.getId());
        }
    }

    @Override
    public List<Long> searchRecords(String modelName, String fieldName, Object value) {
        List<CustomField> fields = fieldMapper.selectByModelName(modelName);
        CustomField field = fields.stream()
                .filter(f -> f.getFieldName().equals(fieldName))
                .findFirst()
                .orElse(null);

        if (field == null) {
            return List.of();
        }

        return searchRecordsByFieldId(field.getId(), value);
    }

    @Override
    public List<Long> searchRecordsByFieldId(Long fieldId, Object value) {
        CustomField field = fieldMapper.selectById(fieldId);
        if (field == null) {
            return List.of();
        }

        switch (field.getFieldType()) {
            case "STRING":
            case "TEXT":
                return valueMapper.searchByStringValue(fieldId, "%" + value.toString() + "%");
            case "INTEGER":
                return valueMapper.searchByIntegerValue(fieldId, (Integer) value);
            case "BOOLEAN":
                return valueMapper.searchByBooleanValue(fieldId, (Boolean) value);
            default:
                return List.of();
        }
    }

    @Override
    @Transactional
    public void copyFieldValues(String modelName, Long sourceRecordId, Long targetRecordId) {
        List<CustomFieldValue> sourceValues = valueMapper.selectByRecord(modelName, sourceRecordId);
        for (CustomFieldValue sourceValue : sourceValues) {
            CustomFieldValue targetValue = new CustomFieldValue();
            targetValue.setFieldId(sourceValue.getFieldId());
            targetValue.setModelName(sourceValue.getModelName());
            targetValue.setRecordId(targetRecordId);
            targetValue.setValueString(sourceValue.getValueString());
            targetValue.setValueInteger(sourceValue.getValueInteger());
            targetValue.setValueLong(sourceValue.getValueLong());
            targetValue.setValueDouble(sourceValue.getValueDouble());
            targetValue.setValueBoolean(sourceValue.getValueBoolean());
            targetValue.setValueDate(sourceValue.getValueDate());
            targetValue.setValueText(sourceValue.getValueText());
            targetValue.setValueJson(sourceValue.getValueJson());
            valueMapper.insert(targetValue);
        }
    }

    private void setValueByType(CustomFieldValue value, String fieldType, Object fieldValue) {
        if (fieldValue == null) {
            return;
        }

        switch (fieldType) {
            case "STRING":
                value.setValueString(fieldValue.toString());
                break;
            case "INTEGER":
                value.setValueInteger((Integer) fieldValue);
                break;
            case "LONG":
                value.setValueLong((Long) fieldValue);
                break;
            case "DOUBLE":
                value.setValueDouble((Double) fieldValue);
                break;
            case "BOOLEAN":
                value.setValueBoolean((Boolean) fieldValue);
                break;
            case "DATE":
            case "DATETIME":
                if (fieldValue instanceof LocalDateTime) {
                    value.setValueDate((LocalDateTime) fieldValue);
                } else if (fieldValue instanceof String) {
                    value.setValueDate(LocalDateTime.parse((String) fieldValue));
                }
                break;
            case "TEXT":
                value.setValueText(fieldValue.toString());
                break;
            case "JSON":
                value.setValueJson(JSONUtil.toJsonStr(fieldValue));
                break;
            case "SELECTION":
                value.setValueString(fieldValue.toString());
                break;
            case "MULTI_SELECTION":
                value.setValueJson(JSONUtil.toJsonStr(fieldValue));
                break;
            default:
                value.setValueString(fieldValue.toString());
        }
    }

    private Object getValueByType(CustomFieldValue value, String fieldType) {
        switch (fieldType) {
            case "STRING":
                return value.getValueString();
            case "INTEGER":
                return value.getValueInteger();
            case "LONG":
                return value.getValueLong();
            case "DOUBLE":
                return value.getValueDouble();
            case "BOOLEAN":
                return value.getValueBoolean();
            case "DATE":
            case "DATETIME":
                return value.getValueDate();
            case "TEXT":
                return value.getValueText();
            case "JSON":
                return JSONUtil.parseObj(value.getValueJson());
            case "SELECTION":
                return value.getValueString();
            case "MULTI_SELECTION":
                return JSONUtil.parseArray(value.getValueJson());
            default:
                return value.getValueString();
        }
    }
}