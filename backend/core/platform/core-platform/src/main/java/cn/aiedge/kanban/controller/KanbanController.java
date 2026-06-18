package cn.aiedge.kanban.controller;

import cn.aiedge.kanban.dto.*;
import cn.aiedge.kanban.entity.KanbanCard;
import cn.aiedge.kanban.entity.KanbanColumn;
import cn.aiedge.kanban.service.KanbanService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "看板视图管理", description = "Odoo核心特性：卡片式任务管理视图")
@RestController
@RequestMapping("/api/kanban")
@RequiredArgsConstructor
public class KanbanController {

    private final KanbanService kanbanService;

    @Operation(summary = "获取看板数据")
    @GetMapping("/{modelName}/data")
    public ResponseEntity<Map<String, Object>> getKanbanData(
            @PathVariable String modelName,
            @RequestParam(defaultValue = "state") String groupField,
            @RequestParam(required = false) String search) {
        KanbanDataResult result = kanbanService.getKanbanData(modelName, groupField, search);
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "创建列")
    @PostMapping("/column")
    public ResponseEntity<Map<String, Object>> createColumn(@RequestBody ColumnCreateRequest request) {
        KanbanColumn column = kanbanService.createColumn(request);
        return ResponseEntity.ok(success(column));
    }

    @Operation(summary = "更新列")
    @PutMapping("/column/{id}")
    public ResponseEntity<Map<String, Object>> updateColumn(@PathVariable Long id, @RequestBody ColumnCreateRequest request) {
        KanbanColumn column = kanbanService.updateColumn(id, request);
        return ResponseEntity.ok(success(column));
    }

    @Operation(summary = "删除列")
    @DeleteMapping("/column/{id}")
    public ResponseEntity<Map<String, Object>> deleteColumn(@PathVariable Long id) {
        kanbanService.deleteColumn(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "获取模型的列")
    @GetMapping("/{modelName}/columns")
    public ResponseEntity<Map<String, Object>> getColumns(
            @PathVariable String modelName,
            @RequestParam(defaultValue = "state") String groupField) {
        List<KanbanColumn> columns = kanbanService.getColumnsByModel(modelName, groupField);
        return ResponseEntity.ok(success(columns));
    }

    @Operation(summary = "创建卡片")
    @PostMapping("/card")
    public ResponseEntity<Map<String, Object>> createCard(@RequestBody CardCreateRequest request) {
        KanbanCard card = kanbanService.createCard(request);
        return ResponseEntity.ok(success(card));
    }

    @Operation(summary = "更新卡片")
    @PutMapping("/card/{id}")
    public ResponseEntity<Map<String, Object>> updateCard(@PathVariable Long id, @RequestBody CardCreateRequest request) {
        KanbanCard card = kanbanService.updateCard(id, request);
        return ResponseEntity.ok(success(card));
    }

    @Operation(summary = "删除卡片")
    @DeleteMapping("/card/{id}")
    public ResponseEntity<Map<String, Object>> deleteCard(@PathVariable Long id) {
        kanbanService.deleteCard(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "移动卡片")
    @PostMapping("/card/move")
    public ResponseEntity<Map<String, Object>> moveCard(@RequestBody CardMoveRequest request) {
        kanbanService.moveCard(request);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "获取列的卡片")
    @GetMapping("/column/{columnId}/cards")
    public ResponseEntity<Map<String, Object>> getCardsByColumn(@PathVariable Long columnId) {
        List<KanbanCard> cards = kanbanService.getCardsByColumn(columnId);
        return ResponseEntity.ok(success(cards));
    }

    @Operation(summary = "卡片列表查询")
    @GetMapping("/card/list")
    public ResponseEntity<Map<String, Object>> listCards(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) Long columnId) {
        Page<KanbanCard> pageResult = kanbanService.listCards(page, size, modelName, columnId);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "获取记录的卡片")
    @GetMapping("/{modelName}/record/{recordId}/card")
    public ResponseEntity<Map<String, Object>> getCardByRecord(
            @PathVariable String modelName,
            @PathVariable Long recordId) {
        KanbanCard card = kanbanService.getCardByRecord(modelName, recordId);
        return ResponseEntity.ok(success(card));
    }

    @Operation(summary = "同步模型数据到看板")
    @PostMapping("/{modelName}/sync")
    public ResponseEntity<Map<String, Object>> syncCards(
            @PathVariable String modelName,
            @RequestParam(defaultValue = "state") String groupField) {
        kanbanService.syncCardsFromModel(modelName, groupField);
        return ResponseEntity.ok(success(null));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}