package cn.aiedge.customfield.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomFieldDTO {

    private Long id;

    private String fieldCode;

    private String modelName;

    private String fieldName;

    private String fieldType;

    private String fieldLabel;

    private Boolean required;

    private Boolean readonly;

    private Boolean searchable;

    private Boolean sortable;

    private String defaultValue;

    private List<SelectionOption> selectionValues;

    private String validationRule;

    private String helpText;

    private String placeholder;

    private Integer sortOrder;

    private String groupCode;

    private String groupName;

    private Boolean active;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}