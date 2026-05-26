package cn.aiedge.kanban.service.impl;

import cn.aiedge.kanban.dto.*;
import cn.aiedge.kanban.entity.KanbanCard;
import cn.aiedge.kanban.entity.KanbanColumn;
import cn.aiedge.kanban.mapper.KanbanCardMapper;
import cn.aiedge.kanban.mapper.KanbanColumnMapper;
import cn.aiedge.kanban.service.KanbanService;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KanbanServiceImpl implements KanbanService {

    private final KanbanColumnMapper columnMapper;
    private final KanbanCardMapper cardMapper;

    @Override
    public KanbanDataResult getKanbanData(String modelName, String groupField, String search) {
        List<KanbanColumn> columns = columnMapper.selectByModelAndGroupField(modelName, groupField);

        List<KanbanColumnDTO> columnDTOs = columns.stream()
                .map(col -> {
                    KanbanColumnDTO dto = convertColumnToDTO(col);
                    List<KanbanCard> cards = cardMapper.selectByColumn(col.getId());

                    if (StrUtil.isNotBlank(search)) {
                        cards = cards.stream()
                                .filter(card -> card.getTitle().contains(search) ||
                                        (card.getDescription() != null && card.getDescription().contains(search)))
                                .collect(Collectors.toList());
                    }

                    dto.setCards(cards.stream()
                            .map(this::convertCardToDTO)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());

        KanbanDataResult result = new KanbanDataResult();
        result.setModelName(modelName);
        result.setGroupField(groupField);
        result.setColumns(columnDTOs);
        result.setTotal(columnDTOs.stream()
                .mapToInt(col -> col.getCards().size())
                .sum());

        return result;
    }

    @Override
    @Transactional
    public KanbanColumn createColumn(ColumnCreateRequest request) {
        KanbanColumn column = new KanbanColumn();
        column.setColumnCode("COL" + IdUtil.fastSimpleUUID().substring(0, 8));
        column.setModelName(request.getModelName());
        column.setGroupField(request.getGroupField());
        column.setGroupValue(request.getGroupValue());
        column.setColumnName(request.getColumnName());
        column.setColor(request.getColor() != null ? request.getColor() : "#1890ff");
        column.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        column.setActive(true);
        columnMapper.insert(column);
        return column;
    }

    @Override
    @Transactional
    public KanbanColumn updateColumn(Long id, ColumnCreateRequest request) {
        KanbanColumn column = columnMapper.selectById(id);
        if (column == null) {
            throw new RuntimeException("列不存在: " + id);
        }
        column.setColumnName(request.getColumnName());
        column.setColor(request.getColor());
        column.setSortOrder(request.getSortOrder());
        columnMapper.updateById(column);
        return column;
    }

    @Override
    @Transactional
    public void deleteColumn(Long id) {
        List<KanbanCard> cards = cardMapper.selectByColumn(id);
        for (KanbanCard card : cards) {
            cardMapper.deleteById(card.getId());
        }
        columnMapper.deleteById(id);
    }

    @Override
    @Transactional
    public KanbanCard createCard(CardCreateRequest request) {
        KanbanCard card = new KanbanCard();
        card.setCardCode("CARD" + IdUtil.fastSimpleUUID().substring(0, 8));
        card.setModelName(request.getModelName());
        card.setRecordId(request.getRecordId());
        card.setColumnId(request.getColumnId());
        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());
        card.setPriority(request.getPriority());
        card.setAssigneeId(request.getAssigneeId());
        if (request.getDueDate() != null) {
            card.setDueDate(LocalDateTime.parse(request.getDueDate()));
        }
        card.setTags(request.getTags());
        card.setSortOrder(cardMapper.countByColumn(request.getColumnId()));
        card.setStatus("ACTIVE");
        cardMapper.insert(card);
        return card;
    }

    @Override
    @Transactional
    public KanbanCard updateCard(Long id, CardCreateRequest request) {
        KanbanCard card = cardMapper.selectById(id);
        if (card == null) {
            throw new RuntimeException("卡片不存在: " + id);
        }
        card.setTitle(request.getTitle());
        card.setDescription(request.getDescription());
        card.setPriority(request.getPriority());
        card.setAssigneeId(request.getAssigneeId());
        if (request.getDueDate() != null) {
            card.setDueDate(LocalDateTime.parse(request.getDueDate()));
        }
        card.setTags(request.getTags());
        if (request.getColumnId() != null) {
            card.setColumnId(request.getColumnId());
        }
        cardMapper.updateById(card);
        return card;
    }

    @Override
    @Transactional
    public void deleteCard(Long id) {
        cardMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void moveCard(CardMoveRequest request) {
        KanbanCard card = cardMapper.selectById(request.getCardId());
        if (card == null) {
            throw new RuntimeException("卡片不存在: " + request.getCardId());
        }

        card.setColumnId(request.getToColumnId());
        if (request.getNewSortOrder() != null) {
            card.setSortOrder(request.getNewSortOrder());
        } else {
            card.setSortOrder(cardMapper.countByColumn(request.getToColumnId()));
        }
        cardMapper.updateById(card);

        log.info("卡片移动: {} 从列 {} 到列 {}", request.getCardId(), request.getFromColumnId(), request.getToColumnId());
    }

    @Override
    public List<KanbanColumn> getColumnsByModel(String modelName, String groupField) {
        return columnMapper.selectByModelAndGroupField(modelName, groupField);
    }

    @Override
    public List<KanbanCard> getCardsByColumn(Long columnId) {
        return cardMapper.selectByColumn(columnId);
    }

    @Override
    public Page<KanbanCard> listCards(Integer page, Integer size, String modelName, Long columnId) {
        Page<KanbanCard> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<KanbanCard> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(modelName)) {
            wrapper.eq(KanbanCard::getModelName, modelName);
        }
        if (columnId != null) {
            wrapper.eq(KanbanCard::getColumnId, columnId);
        }
        wrapper.orderByAsc(KanbanCard::getSortOrder);
        return cardMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public KanbanCard getCardByRecord(String modelName, Long recordId) {
        return cardMapper.selectByRecord(modelName, recordId);
    }

    @Override
    @Transactional
    public void syncCardsFromModel(String modelName, String groupField) {
        log.info("同步模型 {} 的看板卡片，分组字段: {}", modelName, groupField);
    }

    private KanbanColumnDTO convertColumnToDTO(KanbanColumn column) {
        KanbanColumnDTO dto = new KanbanColumnDTO();
        dto.setId(column.getId());
        dto.setColumnCode(column.getColumnCode());
        dto.setModelName(column.getModelName());
        dto.setGroupField(column.getGroupField());
        dto.setGroupValue(column.getGroupValue());
        dto.setColumnName(column.getColumnName());
        dto.setColor(column.getColor());
        dto.setSortOrder(column.getSortOrder());
        dto.setActive(column.getActive());
        dto.setCards(new ArrayList<>());
        return dto;
    }

    private KanbanCardDTO convertCardToDTO(KanbanCard card) {
        KanbanCardDTO dto = new KanbanCardDTO();
        dto.setId(card.getId());
        dto.setCardCode(card.getCardCode());
        dto.setModelName(card.getModelName());
        dto.setRecordId(card.getRecordId());
        dto.setColumnId(card.getColumnId());
        dto.setTitle(card.getTitle());
        dto.setDescription(card.getDescription());
        dto.setPriority(card.getPriority());
        dto.setAssigneeId(card.getAssigneeId());
        dto.setAssigneeName(card.getAssigneeName());
        dto.setDueDate(card.getDueDate());
        if (card.getTags() != null) {
            dto.setTags(JSONUtil.parseArray(card.getTags()).toList(String.class));
        }
        dto.setSortOrder(card.getSortOrder());
        dto.setCreateTime(card.getCreateTime());
        dto.setUpdateTime(card.getUpdateTime());
        return dto;
    }
}