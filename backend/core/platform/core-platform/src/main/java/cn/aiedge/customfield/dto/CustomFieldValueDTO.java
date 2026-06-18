package cn.aiedge.customfield.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CustomFieldValueDTO {

    private Long fieldId;

    private String fieldName;

    private String fieldType;

    private String modelName;

    private Long recordId;

    private Object value;
}