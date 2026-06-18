package cn.aiedge.customfield.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CustomFieldCreateRequest {

    private String modelName;

    private String fieldName;

    private String fieldType;

    private String fieldLabel;

    private Boolean required;

    private Boolean readonly;

    private Boolean searchable;

    private Boolean sortable;

    private String defaultValue;

    private String selectionValues;

    private String validationRule;

    private String helpText;

    private String placeholder;

    private Integer sortOrder;

    private String groupCode;
}