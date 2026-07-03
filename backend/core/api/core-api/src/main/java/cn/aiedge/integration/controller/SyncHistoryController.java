package cn.aiedge.integration.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.integration.mapper.SyncHistoryMapper;
import cn.aiedge.integration.model.SyncHistory;
import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 同步历史控制器
 */
@RestController
@RequestMapping("/api/v1/sync-history")
@RequiredArgsConstructor
@SaCheckLogin
@Tag(name = "同步历史", description = "查看同步执行记录")
public class SyncHistoryController {

    private final SyncHistoryMapper historyMapper;

    @GetMapping
    @Operation(summary = "查询最近的同步历史（所有配置）")
    public ResponseEntity<ApiResponse<List<SyncHistory>>> listRecent() {
        return ResponseEntity.ok(ApiResponse.success(historyMapper.selectRecent()));
    }

    @GetMapping("/config/{configId}")
    @Operation(summary = "查询指定配置的同步历史")
    public ResponseEntity<ApiResponse<List<SyncHistory>>> listByConfig(@PathVariable Long configId) {
        return ResponseEntity.ok(ApiResponse.success(historyMapper.selectByConfigId(configId)));
    }
}
