package cn.aiedge.kanban.dto;

import lombok.Data;

import java.util.List;

@Data
public class KanbanDataResult {

    private String modelName;

    private String groupField;

    private List<KanbanColumnDTO> columns;

    private Integer total;
}