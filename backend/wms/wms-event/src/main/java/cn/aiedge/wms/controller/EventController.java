package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsEventOutbox;
import cn.aiedge.wms.event.service.EventService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@Tag(name = "事件集成")
@RestController
@RequestMapping("/api/wms/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventOutboxService;

    @Operation(summary = "分页查询事件发件箱")
    @GetMapping("/outbox/page")
    public Result<Page<WmsEventOutbox>> outboxPage(Page<WmsEventOutbox> page, WmsEventOutbox query) {
        return Result.ok(eventOutboxService.page(page, query));
    }

    @Operation(summary = "手动重试失败事件")
    @PostMapping("/outbox/retry")
    public Result<String> retryOutbox(@RequestParam @NotNull Long eventId) {
        eventOutboxService.retryEvent(eventId);
        log.info("手动重试事件: eventId={}", eventId);
        return Result.ok("重试成功");
    }

    @Operation(summary = "触发处理待处理事件")
    @PostMapping("/outbox/process")
    public Result<String> processPending() {
        eventOutboxService.processPendingEvents();
        log.info("触发处理待处理事件");
        return Result.ok("处理完成");
    }
}
