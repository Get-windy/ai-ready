package cn.aiedge.erp.finance.controller;

import cn.aiedge.erp.finance.model.entity.FinanceAccount;
import cn.aiedge.erp.finance.service.FinanceAccountService;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财务账户Controller
 *
 * ⚠️ 鉴权易错点：本系统鉴权走 sa-token（登录拦截器 + StpInterface）。
 *    请勿给本 Controller 方法加 Spring Security 的 @PreAuthorize —— sa-token 登录
 *    不会填充 Spring Security 的 SecurityContext，@PreAuthorize 恒抛
 *    AccessDeniedException，且 GlobalExceptionHandler 未专门处理它，会兜底返回 500。
 *    这里曾因此导致 /list 等接口恒 500（提存表单账户下拉取不到数据），已移除
 *    @PreAuthorize（接口仍受 sa-token 登录保护）。
 */
@Tag(name = "财务账户管理", description = "财务账户管理接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/account")
public class FinanceAccountController {
    
    @Autowired
    private FinanceAccountService financeAccountService;
    
    /**
     * 查询财务账户列表
     */
    @OperationLog(module = "财务账户管理", type = "QUERY", desc = "查询财务账户列表")
    @GetMapping("/list")
    public Result<List<FinanceAccount>> listAccounts(
            @Parameter(description = "账户状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "账户类型") @RequestParam(required = false) String accountType) {
        List<FinanceAccount> accounts = financeAccountService.listAccounts(status, accountType);
        return Result.success(accounts);
    }
    
    /**
     * 查询账户统计信息
     */
    @OperationLog(module = "财务账户管理", type = "QUERY", desc = "查询账户统计信息")
    @GetMapping("/statistics")
    public Result<Object> getAccountStatistics() {
        Object statistics = financeAccountService.getAccountStatistics();
        return Result.success(statistics);
    }
    
    /**
     * 启用/停用账户
     */
    @OperationLog(module = "财务账户管理", type = "UPDATE", desc = "更新账户状态")
    @PutMapping("/status/{id}")
    public Result<Void> updateAccountStatus(
            @Parameter(description = "账户ID") @PathVariable Long id,
            @Parameter(description = "状态(0-停用,1-启用)") @RequestBody Integer status) {
        boolean success = financeAccountService.updateAccountStatus(id, status);
        return success ? Result.success() : Result.error("更新失败");
    }
    
    /**
     * 更新账户余额
     */
    @OperationLog(module = "财务账户管理", type = "UPDATE", desc = "更新账户余额")
    @PutMapping("/balance/{accountId}")
    public Result<Void> updateAccountBalance(
            @Parameter(description = "账户ID") @PathVariable Long accountId,
            @Parameter(description = "金额") @RequestBody BigDecimal amount) {
        boolean success = financeAccountService.updateAccountBalance(accountId, amount);
        return success ? Result.success() : Result.error("更新失败");
    }
}
