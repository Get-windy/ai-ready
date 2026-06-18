package cn.aiedge.customfield.service;

import cn.aiedge.customfield.dto.*;
import cn.aiedge.customfield.entity.CustomFieldValue;

import java.util.List;
import java.util.Map;

public interface CustomFieldValueService {

    void saveFieldValue(String modelName, Long recordId, Long fieldId, Object value);

    void saveFieldValues(String modelName, Long recordId, Map<Long, Object> fieldValues);

    void saveFieldValuesByName(String modelName, Long recordId, Map<String, Object> fieldValues);

    Object getFieldValue(String modelName, Long recordId, Long fieldId);

    Object getFieldValueByName(String modelName, Long recordId, String fieldName);

    Map<String, Object> getAllFieldValues(String modelName, Long recordId);

    List<CustomFieldValue> getFieldValuesByRecord(String modelName, Long recordId);

    void deleteFieldValues(String modelName, Long recordId);

    List<Long> searchRecords(String modelName, String fieldName, Object value);

    List<Long> searchRecordsByFieldId(Long fieldId, Object value);

    void copyFieldValues(String modelName, Long sourceRecordId, Long targetRecordId);
}