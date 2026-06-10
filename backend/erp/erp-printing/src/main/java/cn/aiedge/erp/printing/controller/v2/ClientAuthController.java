package cn.aiedge.erp.printing.controller.v2;

import cn.aiedge.base.entity.User;
import cn.aiedge.base.entity.SysTenant;
import cn.aiedge.base.mapper.TenantMapper;
import cn.aiedge.base.mapper.UserMapper;
import cn.aiedge.base.security.PasswordEncryptor;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.printing.dto.v2.*;
import cn.aiedge.erp.printing.entity.v2.SysPrintClient;
import cn.aiedge.erp.printing.service.PrintClientService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 打印客户端认证控制器
 * <p>
 * 提供给 Electron 打印客户端的独立登录接口。
 * 租户用户可通过此接口登录，不受 Web 端登录影响。
 * 登录后可注册/关联打印客户端，后续使用 authKey 进行任务通信。
 */
@Slf4j
@Tag(name = "V2-客户端认证", description = "打印客户端远程登录、注册、自动登录（独立于 Web 端登录）")
@RestController
@RequestMapping("/api/v2/print/client/auth")
@RequiredArgsConstructor
public class ClientAuthController {

    private final UserMapper userMapper;
    private final TenantMapper tenantMapper;
    private final PasswordEncryptor passwordEncryptor;
    private final PrintClientService clientService;

    @Operation(summary = "打印客户端登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(@Valid @RequestBody PrintClientAuthDTO dto) {
        // 1. 查找租户
        SysTenant tenant = tenantMapper.selectByTenantName(dto.getTenantName());
        if (tenant == null) {
            tenant = tenantMapper.selectByTenantCode(dto.getTenantName());
        }
        if (tenant == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "租户不存在"));
        }
        Long tenantId = tenant.getId();

        // 2. 查找用户
        User user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "用户名或密码错误"));
        }

        // 3. 验证密码
        if (!passwordEncryptor.matches(dto.getPassword(), user.getPassword())) {
            return ResponseEntity.ok(ApiResponse.fail(401, "用户名或密码错误"));
        }

        // 4. 验证租户归属
        Long userTenantId = user.getTenantId();
        if (userTenantId == null || !userTenantId.equals(tenantId)) {
            return ResponseEntity.ok(ApiResponse.fail(401, "该用户不属于当前租户"));
        }

        // 5. 创建登录会话
        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();

        // 6. 记录登录日志
        log.info("打印客户端用户登录成功: username={}, tenantId={}, ip={}",
                dto.getUsername(), tenantId, StpUtil.getLoginId());

        // 7. 查找已注册的客户端（根据 machineId）
        PrintClientLoginVO vo = new PrintClientLoginVO();
        vo.setAccessToken(accessToken);
        vo.setTenantId(tenantId);
        vo.setTenantName(tenant.getTenantName());
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());

        if (StrUtil.isNotBlank(dto.getMachineId())) {
            SysPrintClient existing = clientService.findByMachineId(dto.getMachineId(), tenantId);
            if (existing != null) {
                PrintClientLoginVO.ClientInfo ci = new PrintClientLoginVO.ClientInfo();
                ci.setClientId(existing.getClientId());
                ci.setClientName(existing.getClientName());
                ci.setClientCode(existing.getClientCode());
                ci.setAuthKey(existing.getAuthKey());
                ci.setStatus(existing.getStatus());
                vo.setClient(ci);
            }
        }

        // 8. 如选择记住密码，延长 token 有效期
        if (Boolean.TRUE.equals(dto.getRememberMe())) {
            StpUtil.getTokenSession().set("rememberMe", true);
        }

        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "注册/关联打印客户端")
    @PostMapping("/register")
    @SaCheckLogin
    public ResponseEntity<ApiResponse<Object>> register(
            @Valid @RequestBody PrintClientRegisterDTO dto,
            @RequestParam(required = false) String machineId) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "用户不存在"));
        }
        Long tenantId = user.getTenantId();

        // 调用 service 注册客户端
        PrintClientRegisterRequest registerRequest = new PrintClientRegisterRequest();
        registerRequest.setClientName(dto.getClientName());
        registerRequest.setClientVersion(dto.getClientVersion());

        SysPrintClient client = clientService.registerClient(registerRequest, tenantId, userId, machineId);

        // 构建返回
        PrintClientLoginVO vo = new PrintClientLoginVO();
        vo.setAccessToken(StpUtil.getTokenValue());
        vo.setTenantId(tenantId);
        vo.setUserId(userId);
        vo.setUsername(user.getUsername());

        PrintClientLoginVO.ClientInfo ci = new PrintClientLoginVO.ClientInfo();
        ci.setClientId(client.getClientId());
        ci.setClientName(client.getClientName());
        ci.setClientCode(client.getClientCode());
        ci.setAuthKey(client.getAuthKey());
        ci.setStatus(client.getStatus());
        vo.setClient(ci);

        log.info("打印客户端注册成功: clientId={}, clientName={}, tenantId={}",
                client.getClientId(), dto.getClientName(), tenantId);

        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "获取当前登录用户与客户端信息")
    @GetMapping("/me")
    @SaCheckLogin
    public ResponseEntity<ApiResponse<Object>> me(@RequestParam(required = false) Long clientId) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.fail(401, "用户不存在"));
        }
        Long tenantId = user.getTenantId();

        PrintClientLoginVO vo = new PrintClientLoginVO();
        vo.setAccessToken(StpUtil.getTokenValue());
        vo.setTenantId(tenantId);
        vo.setUserId(userId);
        vo.setUsername(user.getUsername());

        if (clientId != null) {
            try {
                SysPrintClient client = clientService.getClientEntity(clientId, tenantId);
                PrintClientLoginVO.ClientInfo ci = new PrintClientLoginVO.ClientInfo();
                ci.setClientId(client.getClientId());
                ci.setClientName(client.getClientName());
                ci.setClientCode(client.getClientCode());
                ci.setAuthKey(client.getAuthKey());
                ci.setStatus(client.getStatus());
                vo.setClient(ci);
            } catch (Exception e) {
                // client not found, ignore
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(vo));
    }

    @Operation(summary = "检查 Token 有效性")
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Object>> checkToken() {
        boolean isLogin = StpUtil.isLogin();
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "valid", isLogin,
                "userId", isLogin ? StpUtil.getLoginIdAsLong() : null,
                "tokenTimeout", isLogin ? StpUtil.getTokenTimeout() : 0
        )));
    }

    @Operation(summary = "打印客户端登出")
    @PostMapping("/logout")
    @SaCheckLogin
    public ResponseEntity<ApiResponse<Object>> logout() {
        StpUtil.logout();
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
