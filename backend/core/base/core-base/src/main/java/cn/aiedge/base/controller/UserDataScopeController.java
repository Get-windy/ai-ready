package cn.aiedge.base.controller;

import cn.aiedge.base.service.UserDataScopeService;
import cn.aiedge.base.vo.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 操作员数据权限控制器
 * <p>
 * 对标 ql361「资料 → 职员权限 → 全部操作员」页：给「操作员（sys_user）× 维度 × 授权对象清单」补读写能力。
 * 维度白名单见 {@link UserDataScopeService#SCOPE_KEYS}。
 * </p>
 * <p>
 * 鉴权：权限码 {@code data-scope:view} / {@code data-scope:set}（种子见 V11.430.0）。
 * 参数校验失败抛 {@link IllegalArgumentException}，由 GlobalExceptionHandler 统一转 400。
 * </p>
 * <p>
 * 多租户：{@code sys_user_data_scope} 有 tenant_id 且**不在**
 * MyBatisPlusConfig.IGNORE_TENANT_TABLES 白名单内（已核对），
 * 故此处**不手写** tenant_id 条件，由 MyBatis-Plus 租户拦截器自动注入。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "操作员数据权限", description = "操作员维度化数据授权（对象清单）")
@RestController
@RequestMapping("/api/user-data-scope")
@RequiredArgsConstructor
@SaCheckLogin
public class UserDataScopeController {

    private final UserDataScopeService userDataScopeService;

    /**
     * 查询某操作员全部已设置维度的数据权限
     */
    @Operation(summary = "查询操作员全部维度的数据权限")
    @GetMapping("/{userId}")
    @SaCheckPermission("data-scope:view")
    public Result<Map<String, List<String>>> getUserScopes(@PathVariable Long userId) {
        validateUserId(userId);
        return Result.ok(userDataScopeService.getUserScopes(userId));
    }

    /**
     * 覆盖保存某操作员某维度的授权对象清单
     */
    @Operation(summary = "覆盖保存操作员某维度的数据权限")
    @PutMapping("/{userId}/{scopeKey}")
    @SaCheckPermission("data-scope:set")
    public Result<Void> saveScope(@PathVariable Long userId,
                                  @PathVariable String scopeKey,
                                  @RequestBody(required = false) List<String> targetIds) {
        validateUserId(userId);
        validateScopeKey(scopeKey);
        userDataScopeService.saveScope(userId, scopeKey, targetIds);
        return Result.ok("保存成功", null);
    }

    /**
     * 清除某操作员某维度的数据权限
     */
    @Operation(summary = "清除操作员某维度的数据权限")
    @DeleteMapping("/{userId}/{scopeKey}")
    @SaCheckPermission("data-scope:set")
    public Result<Void> clearScope(@PathVariable Long userId,
                                   @PathVariable String scopeKey) {
        validateUserId(userId);
        validateScopeKey(scopeKey);
        userDataScopeService.clearScope(userId, scopeKey);
        return Result.ok("清除成功", null);
    }

    /**
     * 查询某维度下可授权的候选对象清单
     * <p>
     * 注意路径为字面量 {@code /targets}，Spring MVC 中字面量优先于 {@code /{userId}} 变量段，不会冲突。
     * </p>
     */
    @Operation(summary = "查询某维度可授权的候选对象清单")
    @GetMapping("/targets")
    @SaCheckPermission("data-scope:view")
    public Result<List<Map<String, Object>>> listTargets(@RequestParam String scopeKey,
                                                         @RequestParam(required = false) String keyword) {
        validateScopeKey(scopeKey);
        return Result.ok(userDataScopeService.listTargets(scopeKey, keyword));
    }

    /**
     * 校验操作员ID
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("操作员ID不能为空且必须大于0");
        }
    }

    /**
     * 校验维度白名单
     */
    private void validateScopeKey(String scopeKey) {
        if (!userDataScopeService.isValidScopeKey(scopeKey)) {
            throw new IllegalArgumentException("非法的数据权限维度: " + scopeKey);
        }
    }
}
