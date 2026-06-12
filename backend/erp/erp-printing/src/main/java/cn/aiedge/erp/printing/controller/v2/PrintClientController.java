package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.PrintClientRegisterRequest;
import cn.aiedge.erp.printing.dto.v2.PrintClientVO;
import cn.aiedge.erp.printing.service.PrintClientService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "V2-打印客户端管理", description = "Windows 打印客户端注册、状态管理、认证密钥管理")
@RestController
@RequestMapping("/api/v2/print/clients")
@RequiredArgsConstructor
public class PrintClientController {

    private final PrintClientService clientService;

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

    @Operation(summary = "注册打印客户端")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(
            @Valid @RequestBody PrintClientRegisterRequest request,
            @RequestHeader(required = false) Long tenantId,
            @RequestHeader(required = false) Long userId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        Long resolvedUserId = resolveUserId(userId);
        PrintClientVO vo = clientService.register(request, resolvedTenantId, resolvedUserId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "更新客户端信息")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> update(
            @PathVariable Long id,
            @Valid @RequestBody PrintClientRegisterRequest request,
            @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        PrintClientVO vo = clientService.updateClient(id, request, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "获取客户端详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> get(@PathVariable Long id, @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        PrintClientVO vo = clientService.getClient(id, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "客户端列表")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        Page<PrintClientVO> pageResult = clientService.listClients(page, size, status, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(pageResult));
    }

    @Operation(summary = "删除客户端")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id, @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        clientService.deleteClient(id, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "重置认证密钥")
    @PostMapping("/{id}/reset-key")
    public ResponseEntity<ApiResponse<Object>> resetKey(@PathVariable Long id, @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        String newKey = clientService.resetAuthKey(id, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("authKey", newKey)));
    }

    @Operation(summary = "更新客户端状态")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader(required = false) Long tenantId) {
        Long resolvedTenantId = resolveTenantId(tenantId);
        clientService.updateStatus(id, status, resolvedTenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
