package cn.aiedge.kanban.dto;

import lombok.Data;

public class CardMoveRequest {

    private Long cardId;

    private Long fromColumnId;

    private Long toColumnId;

    private Integer newSortOrder;
}