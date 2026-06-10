package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.PrintClientRegisterRequest;
import cn.aiedge.erp.printing.dto.v2.PrintClientVO;
import cn.aiedge.erp.printing.service.PrintClientService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "V2-打印客户端管理", description = "Windows 打印客户端注册、状态管理、认证密钥管理")
@RestController
@RequestMapping("/api/v2/print/clients")
@RequiredArgsConstructor
public class PrintClientController {

    private final PrintClientService clientService;

    @Operation(summary = "注册打印客户端")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(
            @Valid @RequestBody PrintClientRegisterRequest request,
            @RequestHeader Long tenantId,
            @RequestHeader Long userId) {
        PrintClientVO vo = clientService.register(request, tenantId, userId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "更新客户端信息")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> update(
            @PathVariable Long id,
            @Valid @RequestBody PrintClientRegisterRequest request,
            @RequestHeader Long tenantId) {
        PrintClientVO vo = clientService.updateClient(id, request, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "获取客户端详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> get(@PathVariable Long id, @RequestHeader Long tenantId) {
        PrintClientVO vo = clientService.getClient(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "客户端列表")
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String status,
            @RequestHeader Long tenantId) {
        Page<PrintClientVO> pageResult = clientService.listClients(page, size, status, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(pageResult));
    }

    @Operation(summary = "删除客户端")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id, @RequestHeader Long tenantId) {
        clientService.deleteClient(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "重置认证密钥")
    @PostMapping("/{id}/reset-key")
    public ResponseEntity<ApiResponse<Object>> resetKey(@PathVariable Long id, @RequestHeader Long tenantId) {
        String newKey = clientService.resetAuthKey(id, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("authKey", newKey)));
    }

    @Operation(summary = "更新客户端状态")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestHeader Long tenantId) {
        clientService.updateStatus(id, status, tenantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
