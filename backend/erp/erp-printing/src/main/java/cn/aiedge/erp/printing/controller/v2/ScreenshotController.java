package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.ScreenshotCreateRequest;
import cn.aiedge.erp.printing.dto.v2.ScreenshotVO;
import cn.aiedge.erp.printing.service.ScreenshotService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "V2-截图服务", description = "打印模板截图生成、预览图获取，预留微信发送扩展")
@RestController
@RequestMapping("/api/v2/print/screenshots")
@RequiredArgsConstructor
public class ScreenshotController {

    private final ScreenshotService screenshotService;

    @Operation(summary = "创建截图任务")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> create(
            @Valid @RequestBody ScreenshotCreateRequest request,
            @RequestHeader Long tenantId,
            @RequestHeader Long userId) {
        ScreenshotVO vo = screenshotService.createScreenshotTask(request, tenantId, userId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "获取截图详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> get(@PathVariable Long id, @RequestHeader Long tenantId) {
        ScreenshotVO vo = screenshotService.getScreenshot(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "截图任务列表")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestHeader Long tenantId) {
        Page<ScreenshotVO> pageResult = screenshotService.listScreenshots(page, size, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(pageResult));
    }

    @Operation(summary = "前端截图回调 - 上传截图结果")
    @PutMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<Object>> complete(
            @PathVariable Long id,
            @RequestParam(required = false) String imageUrl,
            @RequestParam(required = false) String imageBase64,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            @RequestParam(required = false) Long fileSize) {
        screenshotService.completeScreenshot(id, imageUrl, imageBase64, width, height, fileSize);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "重试截图任务（超时后可重新调起，无需重新打印）")
    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<Object>> retry(@PathVariable Long id) {
        screenshotService.retryScreenshot(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
