package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsPutawayTask;
import cn.aiedge.wms.putaway.service.PutawayService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/warehouse/putaway")
@Tag(name = "PDA-上架")
@RequiredArgsConstructor
public class PdaPutawayController {

    private final PutawayService putawayService;

    private static final long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_USER_NAME = "PDA操作员";

    @Operation(summary = "获取上架任务列表")
    @GetMapping
    public Result<List<WmsPutawayTask>> list() {
        Page<WmsPutawayTask> page = putawayService.pageTask(
                new Page<>(1, 1000), new WmsPutawayTask());
        return Result.ok(page.getRecords());
    }

    @Operation(summary = "获取上架任务详情")
    @GetMapping("/{id}")
    public Result<WmsPutawayTask> detail(@PathVariable @NotNull Long id) {
        WmsPutawayTask task = putawayService.getTaskById(id);
        if (task == null) return Result.fail("上架任务不存在");
        return Result.ok(task);
    }

    @Operation(summary = "扫描商品条码")
    @PostMapping("/{id}/scan")
    public Result<Map<String, Object>> scan(@PathVariable @NotNull Long id,
                                            @RequestBody Map<String, Object> body) {
        String barcode = (String) body.get("barcode");
        log.info("PDA扫码上架: taskId={}, barcode={}", id, barcode);
        return Result.ok(Map.of("barcode", barcode, "scanned", true, "taskId", id));
    }

    @Operation(summary = "确认上架完成")
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable @NotNull Long id) {
        putawayService.confirmPutaway(id, DEFAULT_USER_ID, DEFAULT_USER_NAME);
        log.info("PDA确认上架: taskId={}", id);
        return Result.ok();
    }
}
