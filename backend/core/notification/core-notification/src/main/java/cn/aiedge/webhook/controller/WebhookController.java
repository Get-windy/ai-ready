package cn.aiedge.webhook.controller;

import cn.aiedge.webhook.dto.*;
import cn.aiedge.webhook.entity.Webhook;
import cn.aiedge.webhook.entity.WebhookLog;
import cn.aiedge.webhook.service.WebhookService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Webhook管理", description = "事件推送通知机制")
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @Operation(summary = "创建Webhook")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createWebhook(@RequestBody WebhookCreateRequest request) {
        Webhook webhook = webhookService.createWebhook(request);
        return ResponseEntity.ok(success(webhook));
    }

    @Operation(summary = "更新Webhook")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateWebhook(@PathVariable Long id, @RequestBody WebhookCreateRequest request) {
        Webhook webhook = webhookService.updateWebhook(id, request);
        return ResponseEntity.ok(success(webhook));
    }

    @Operation(summary = "获取Webhook详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getWebhook(@PathVariable Long id) {
        Webhook webhook = webhookService.getWebhookById(id);
        return ResponseEntity.ok(success(webhook));
    }

    @Operation(summary = "获取模型的所有Webhook")
    @GetMapping("/model/{modelName}")
    public ResponseEntity<Map<String, Object>> getWebhooksByModel(@PathVariable String modelName) {
        List<Webhook> webhooks = webhookService.getWebhooksByModel(modelName);
        return ResponseEntity.ok(success(webhooks));
    }

    @Operation(summary = "获取模型特定事件的Webhook")
    @GetMapping("/model/{modelName}/event/{triggerEvent}")
    public ResponseEntity<Map<String, Object>> getWebhooksByModelAndEvent(
            @PathVariable String modelName,
            @PathVariable String triggerEvent) {
        List<Webhook> webhooks = webhookService.getWebhooksByModelAndEvent(modelName, triggerEvent);
        return ResponseEntity.ok(success(webhooks));
    }

    @Operation(summary = "Webhook列表查询")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listWebhooks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String triggerEvent) {
        Page<Webhook> pageResult = webhookService.listWebhooks(page, size, modelName, triggerEvent);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "删除Webhook")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteWebhook(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "激活Webhook")
    @PostMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateWebhook(@PathVariable Long id) {
        webhookService.activateWebhook(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "停用Webhook")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateWebhook(@PathVariable Long id) {
        webhookService.deactivateWebhook(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "手动触发Webhook")
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerWebhook(
            @RequestParam String modelName,
            @RequestParam String triggerEvent,
            @RequestParam Long recordId,
            @RequestBody Map<String, Object> data) {
        webhookService.triggerWebhook(modelName, triggerEvent, recordId, data);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "获取Webhook日志")
    @GetMapping("/{webhookId}/logs")
    public ResponseEntity<Map<String, Object>> getWebhookLogs(
            @PathVariable Long webhookId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<WebhookLog> logs = webhookService.getWebhookLogs(webhookId, limit);
        return ResponseEntity.ok(success(logs));
    }

    @Operation(summary = "日志列表查询")
    @GetMapping("/logs/list")
    public ResponseEntity<Map<String, Object>> listLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long webhookId,
            @RequestParam(required = false) Boolean success) {
        Page<WebhookLog> pageResult = webhookService.listLogs(page, size, webhookId, success);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "重试失败的Webhook")
    @PostMapping("/retry")
    public ResponseEntity<Map<String, Object>> retryFailedWebhooks() {
        webhookService.retryFailedWebhooks();
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "验证签名")
    @PostMapping("/verify-signature")
    public ResponseEntity<Map<String, Object>> verifySignature(
            @RequestParam String secretKey,
            @RequestParam String payload,
            @RequestParam String signature) {
        boolean valid = webhookService.verifySignature(secretKey, payload, signature);
        return ResponseEntity.ok(success(Map.of("valid", valid)));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}