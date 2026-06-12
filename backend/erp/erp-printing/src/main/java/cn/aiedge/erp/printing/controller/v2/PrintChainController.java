package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.PrintChainCreateRequest;
import cn.aiedge.erp.printing.dto.v2.PrintChainVO;
import cn.aiedge.erp.printing.service.PrintChainService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "V2-打印链路管理", description = "打印链路的增删改查，支持多级步骤（1~10级）")
@RestController
@RequestMapping("/api/v2/print/chains")
@RequiredArgsConstructor
public class PrintChainController {

    private final PrintChainService chainService;

    /**
     * 从 Sa-Token 会话或请求头获取 tenantId
     */
    private Long resolveTenantId(Long headerTenantId) {
        if (headerTenantId != null) {
            return headerTenantId;
        }
        try {
            Object sessionTenantId = StpUtil.getSession().get("tenantId");
            if (sessionTenantId instanceof Number) {
                return ((Number) sessionTenantId).longValue();
            }
        } catch (Exception e) {
            log.warn("无法从Sa-Token会话获取tenantId: {}", e.getMessage());
        }
        return 1L; // 默认租户
    }

    /**
     * 从 Sa-Token 会话获取 userId
     */
    private Long resolveUserId(Long headerUserId) {
        if (headerUserId != null) {
            return headerUserId;
        }
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            return 0L;
        }
    }

    @Operation(summary = "创建打印链路（含明细）")
    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createChain(
            @Valid @RequestBody PrintChainCreateRequest request,
            @RequestHeader(required = false) Long tenantId,
            @RequestHeader(required = false) Long userId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        Long resolvedUserId = resolveUserId(userId);
        PrintChainVO vo = chainService.createChain(request, resolvedTenantId, resolvedUserId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "更新打印链路（全量替换明细）")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> updateChain(
            @PathVariable Long id,
            @Valid @RequestBody PrintChainCreateRequest request,
            @RequestHeader(required = false) Long tenantId,
            @RequestHeader(required = false) Long userId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        Long resolvedUserId = resolveUserId(userId);
        PrintChainVO vo = chainService.updateChain(id, request, resolvedTenantId, resolvedUserId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "获取链路详情（含全部明细）")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getChain(@PathVariable Long id, @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        PrintChainVO vo = chainService.getChainDetail(id, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "链路列表查询")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> listChains(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String pageCode,
            @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        Page<PrintChainVO> pageResult = chainService.listChains(page, size, pageCode, resolvedTenantId);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "按 pageCode 获取链路列表")
    @GetMapping("/by-page/{pageCode}")
    public ResponseEntity<ApiResponse<Object>> listByPageCode(
            @PathVariable String pageCode,
            @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        List<PrintChainVO> list = chainService.listByPageCode(pageCode, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @Operation(summary = "删除打印链路")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteChain(@PathVariable Long id, @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        chainService.deleteChain(id, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "启用/禁用链路")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        chainService.updateChainStatus(id, status, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
