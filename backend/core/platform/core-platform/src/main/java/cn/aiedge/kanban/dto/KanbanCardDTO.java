package cn.aiedge.kanban.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class KanbanCardDTO {

    private Long id;

    private String cardCode;

    private String modelName;

    private Long recordId;

    private Long columnId;

    private String title;

    private String description;

    private String priority;

    private Long assigneeId;

    private String assigneeName;

    private LocalDateTime dueDate;

    private List<String> tags;

    private Integer sortOrder;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}