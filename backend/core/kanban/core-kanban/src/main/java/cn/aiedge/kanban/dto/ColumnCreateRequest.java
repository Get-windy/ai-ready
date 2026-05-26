package cn.aiedge.kanban.dto;

import lombok.Data;

public class ColumnCreateRequest {

    private String modelName;

    private String groupField;

    private String groupValue;

    private String columnName;

    private String color;

    private Integer sortOrder;
}