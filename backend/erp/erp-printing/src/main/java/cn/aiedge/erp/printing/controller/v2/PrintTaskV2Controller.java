package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.*;
import cn.aiedge.erp.printing.entity.v2.SysPrintTask;
import cn.aiedge.erp.printing.mapper.SysPrintTaskMapper;
import cn.aiedge.erp.printing.service.ChainExecutorService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "V2-打印任务管理", description = "支持单模板打印和链路打印，含截图确认流程")
@RestController
@RequestMapping("/api/v2/print/tasks")
@RequiredArgsConstructor
public class PrintTaskV2Controller {

    private final ChainExecutorService chainExecutorService;
    private final SysPrintTaskMapper taskMapper;

    @Operation(summary = "按链路执行打印")
    @PostMapping("/by-chain")
    public ResponseEntity<ApiResponse<Object>> executeByChain(
            @Valid @RequestBody ChainTaskExecuteRequest request,
            @RequestHeader Long tenantId,
            @RequestHeader Long userId) {
        List<PrintTaskVO> tasks = chainExecutorService.executeChain(request, tenantId, userId);
        return ResponseEntity.ok(ApiResponse.ok(tasks));
    }

    @Operation(summary = "获取任务详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getTask(@PathVariable Long id, @RequestHeader Long tenantId) {
        SysPrintTask task = taskMapper.selectById(id);
        if (task == null || !task.getTenantId().equals(tenantId)) {
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
        return ResponseEntity.ok(ApiResponse.ok(task));
    }

    @Operation(summary = "任务列表查询")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> listTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String pageCode,
            @RequestHeader Long tenantId) {
        Page<SysPrintTask> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<SysPrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPrintTask::getTenantId, tenantId);
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(SysPrintTask::getStatus, status);
        }
        if (StrUtil.isNotBlank(documentType)) {
            wrapper.eq(SysPrintTask::getDocumentType, documentType);
        }
        if (StrUtil.isNotBlank(pageCode)) {
            wrapper.eq(SysPrintTask::getPageCode, pageCode);
        }
        wrapper.orderByDesc(SysPrintTask::getCreatedAt);

        Page<SysPrintTask> taskPage = taskMapper.selectPage(pageObj, wrapper);
        return ResponseEntity.ok(ApiResponse.ok(taskPage));
    }

    @Operation(summary = "取消任务")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Object>> cancelTask(@PathVariable Long id) {
        chainExecutorService.cancelWaitingTask(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "确认截图并继续打印")
    @PostMapping("/{id}/confirm-screenshot")
    public ResponseEntity<ApiResponse<Object>> confirmScreenshot(@PathVariable Long id) {
        chainExecutorService.confirmScreenshot(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "获取任务队列（PRINTING 状态）")
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<Object>> getQueue(@RequestHeader Long tenantId) {
        LambdaQueryWrapper<SysPrintTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPrintTask::getTenantId, tenantId)
                .in(SysPrintTask::getStatus, "QUEUED", "PRINTING")
                .orderByAsc(SysPrintTask::getPriority)
                .orderByAsc(SysPrintTask::getCreatedAt);
        List<SysPrintTask> queue = taskMapper.selectList(wrapper);
        return ResponseEntity.ok(ApiResponse.ok(queue));
    }
}
