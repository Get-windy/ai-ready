package cn.aiedge.kanban.dto;

import lombok.Data;

@Data
public class CardCreateRequest {

    private String modelName;

    private Long recordId;

    private Long columnId;

    private String title;

    private String description;

    private String priority;

    private Long assigneeId;

    private String dueDate;

    private String tags;
}