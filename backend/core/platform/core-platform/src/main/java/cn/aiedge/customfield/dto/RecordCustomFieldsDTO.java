package cn.aiedge.customfield.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RecordCustomFieldsDTO {

    private String modelName;

    private Long recordId;

    private Map<String, Object> fieldValues;
}