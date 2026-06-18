package cn.aiedge.kanban.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class KanbanColumnDTO {

    private Long id;

    private String columnCode;

    private String modelName;

    private String groupField;

    private String groupValue;

    private String columnName;

    private String color;

    private Integer sortOrder;

    private Boolean active;

    private List<KanbanCardDTO> cards;
}