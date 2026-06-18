package cn.aiedge.kanban.service;

import cn.aiedge.kanban.dto.*;
import cn.aiedge.kanban.entity.KanbanCard;
import cn.aiedge.kanban.entity.KanbanColumn;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface KanbanService {

    KanbanDataResult getKanbanData(String modelName, String groupField, String search);

    KanbanColumn createColumn(ColumnCreateRequest request);

    KanbanColumn updateColumn(Long id, ColumnCreateRequest request);

    void deleteColumn(Long id);

    KanbanCard createCard(CardCreateRequest request);

    KanbanCard updateCard(Long id, CardCreateRequest request);

    void deleteCard(Long id);

    void moveCard(CardMoveRequest request);

    List<KanbanColumn> getColumnsByModel(String modelName, String groupField);

    List<KanbanCard> getCardsByColumn(Long columnId);

    Page<KanbanCard> listCards(Integer page, Integer size, String modelName, Long columnId);

    KanbanCard getCardByRecord(String modelName, Long recordId);

    void syncCardsFromModel(String modelName, String groupField);
}