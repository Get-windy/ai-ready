package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.*;
import cn.aiedge.erp.printing.entity.v2.SysPrintClient;
import cn.aiedge.erp.printing.entity.v2.SysPrintTask;
import cn.aiedge.erp.printing.entity.v2.SysPrintTemplate;
import cn.aiedge.erp.printing.engine.FormatEngine;
import cn.aiedge.erp.printing.mapper.SysPrintTaskMapper;
import cn.aiedge.erp.printing.mapper.SysPrintTemplateMapper;
import cn.aiedge.erp.printing.service.PrintClientService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 客户端专用 API —— 由 Windows 打印客户端直接调用，使用 auth_key 认证
 */
@Tag(name = "客户端 API", description = "Windows 打印客户端专用接口：任务轮询、状态上报、心跳")
@RestController
@RequestMapping("/api/v2/print/client")
@RequiredArgsConstructor
public class ClientApiController {

    private final PrintClientService clientService;
    private final SysPrintTaskMapper taskMapper;
    private final SysPrintTemplateMapper templateMapper;
    private final FormatEngine formatEngine;

    @Operation(summary = "客户端心跳上报")
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Object>> heartbeat(
            @Valid @RequestBody HeartbeatRequest request,
            @RequestHeader(required = false) String xForwardedFor) {
        String clientIp = xForwardedFor != null ? xForwardedFor : "unknown";
        clientService.processHeartbeat(
                request.getClientId(),
                request.getAuthKey(),
                request.getClientVersion(),
                request.getDefaultPrinter(),
                clientIp
        );
        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "OK")));
    }

    @Operation(summary = "客户端轮询拉取待处理任务")
    @PostMapping("/tasks/poll")
    public ResponseEntity<ApiResponse<Object>> pollTasks(
            @RequestParam Long clientId,
            @RequestParam String authKey,
            @RequestParam(defaultValue = "10") Integer limit) {
        // 鉴权
        clientService.authenticate(clientId, authKey);

        // 查询待处理任务
        LambdaQueryWrapper<SysPrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPrintTask::getClientId, clientId)
                .eq(SysPrintTask::getStatus, "PRINTING")
                .orderByAsc(SysPrintTask::getPriority)
                .orderByAsc(SysPrintTask::getCreatedAt)
                .last("LIMIT " + Math.min(limit, 50));

        List<SysPrintTask> tasks = taskMapper.selectList(wrapper);

        // 转换为客户端响应格式
        List<ClientTaskItem> items = tasks.stream().map(task -> {
            ClientTaskItem item = new ClientTaskItem();
            item.setTaskId(task.getTaskId());
            item.setTaskCode(task.getTaskCode());
            item.setPrinterName(task.getPrinterName());

            // 解析模板和数据，预渲染 HTML
            if (task.getTemplateId() != null) {
                SysPrintTemplate template = templateMapper.selectById(task.getTemplateId());
                if (template != null) {
                    try {
                        String html = formatEngine.renderToHtml(
                                template.getTemplateJson(),
                                task.getDataJson() != null ? task.getDataJson() : "{}"
                        );
                        item.setRenderedHtml(html);
                    } catch (Exception e) {
                        item.setRenderedHtml("<html><body>渲染错误</body></html>");
                    }
                }
            }

            // 标记任务已分发
            task.setStatus("PRINTING");
            task.setStartTime(LocalDateTime.now());
            taskMapper.updateById(task);

            return item;
        }).collect(Collectors.toList());

        ClientTaskPollResponse response = new ClientTaskPollResponse();
        response.setTasks(items);
        response.setHasMore(items.size() >= limit);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Operation(summary = "客户端上报任务状态")
    @PutMapping("/tasks/status")
    public ResponseEntity<ApiResponse<Object>> reportStatus(
            @Valid @RequestBody TaskStatusReportRequest request,
            @RequestParam Long clientId,
            @RequestParam String authKey) {
        // 鉴权
        clientService.authenticate(clientId, authKey);

        SysPrintTask task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "NOT_FOUND")));
        }

        task.setStatus(request.getStatus());
        task.setErrorMessage(request.getErrorMessage());
        task.setResultLog(request.getResultLog());

        if ("COMPLETED".equals(request.getStatus())) {
            task.setCompleteTime(LocalDateTime.now());
        } else if ("FAILED".equals(request.getStatus())) {
            task.setErrorMessage(request.getErrorMessage());
            // 自动重试逻辑
            if (task.getRetryCount() < task.getMaxRetry()) {
                task.setRetryCount(task.getRetryCount() + 1);
                task.setStatus("QUEUED");
            } else {
                task.setCompleteTime(LocalDateTime.now());
            }
        }

        taskMapper.updateById(task);

        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "UPDATED")));
    }

    @Operation(summary = "客户端截图确认回调")
    @PostMapping("/screenshots/{screenshotId}/confirm")
    public ResponseEntity<ApiResponse<Object>> confirmScreenshot(
            @PathVariable Long screenshotId,
            @RequestParam Long clientId,
            @RequestParam String authKey,
            @RequestParam String imageBase64) {
        clientService.authenticate(clientId, authKey);

        // 查找关联的 task
        LambdaQueryWrapper<SysPrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPrintTask::getScreenshotId, screenshotId);
        SysPrintTask task = taskMapper.selectOne(wrapper);

        if (task != null) {
            task.setResultLog((task.getResultLog() != null ? task.getResultLog() : "")
                    + "\n[截图已上传并确认，clientId=" + clientId + "]");
            task.setStatus("QUEUED");
            taskMapper.updateById(task);
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of("status", "CONFIRMED")));
    }
}
