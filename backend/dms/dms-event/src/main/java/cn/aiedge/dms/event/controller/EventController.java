package cn.aiedge.dms.event.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.event.entity.DmsEventOutbox;
import cn.aiedge.dms.event.service.EventService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 事件集成控制器
 *
 * 提供事件发件箱的查询和手动重试等管理接口。
 *
 * @author AI-Ready Team
 */
@Tag(name = "事件集成")
@RestController
@RequestMapping("/api/dms/event")
@RequiredArgsConstructor
@SaCheckLogin
public class EventController {

    private final EventService eventService;
    private final cn.aiedge.dms.event.mapper.DmsEventOutboxMapper eventOutboxMapper;

    @Operation(summary = "查询待发送事件列表")
    @GetMapping("/pending")
    public ApiResponse<List<DmsEventOutbox>> getPendingEvents() {
        List<DmsEventOutbox> pendingEvents = eventOutboxMapper.findPendingEvents();
        return ApiResponse.success(pendingEvents);
    }

    @Operation(summary = "手动重试失败事件")
    @PostMapping("/{id}/retry")
    @SaCheckPermission("dms:event:retry")
    public ApiResponse<Void> manualRetry(
            @Parameter(description = "事件ID") @PathVariable Long id) {
        eventService.retryFailedEvents();
        return ApiResponse.success();
    }
}
