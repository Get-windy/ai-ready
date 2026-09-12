package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.service.UserService;
import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.MonthClosingResultDTO;
import cn.aiedge.erp.finance.model.entity.MonthClosingLog;
import cn.aiedge.erp.finance.service.MonthClosingService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 月结Controller
 */
@Tag(name = "总账月结", description = "月结执行、反月结、状态与日志查询接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/month-closing")
@RequiredArgsConstructor
public class MonthClosingController {

    private final MonthClosingService monthClosingService;
    private final UserService userService;

    /**
     * 解析操作人：优先取 Sa-Token 登录用户（ID + Session用户名/sys_user真实姓名），请求头仅作兜底。
     * @return [operatorId, operatorName]
     */
    private String[] resolveOperator(String headerUserId, String headerUsername) {
        String operatorId = headerUserId;
        String operatorName = headerUsername;
        Long loginId = SecurityUtils.getCurrentUserId();
        if (loginId != null) {
            operatorId = String.valueOf(loginId);
            operatorName = SecurityUtils.getCurrentUsername();
            if (operatorName == null || operatorName.isEmpty()) {
                try {
                    SysUser user = userService.getById(loginId);
                    if (user != null) {
                        operatorName = user.getRealName() != null && !user.getRealName().isEmpty()
                                ? user.getRealName() : user.getUsername();
                    }
                } catch (Exception e) {
                    log.warn("查询操作人姓名失败: userId={}", loginId, e);
                }
            }
            if (operatorName == null || operatorName.isEmpty()) {
                operatorName = headerUsername;
            }
        }
        if (operatorId == null || operatorId.isEmpty()) {
            operatorId = "mock_operator";
        }
        return new String[]{operatorId, operatorName};
    }

    @Operation(summary = "执行月结")
    @PostMapping("/execute")
    @PreAuthorize("hasPermission('/api/erp/finance/month-closing/execute', 'finance:month-closing:execute')")
    @OperationLog(module = "总账月结", type = "UPDATE", desc = "执行月结")
    public Result<MonthClosingResultDTO> execute(
            @Parameter(description = "期间编码 yyyy-MM") @RequestParam String periodCode,
            @Parameter(description = "操作人ID") @RequestHeader(value = "userId", required = false) String operatorId,
            @Parameter(description = "操作人姓名") @RequestHeader(value = "username", required = false) String operatorName) {
        String[] operator = resolveOperator(operatorId, operatorName);
        return Result.success(monthClosingService.execute(periodCode, operator[0], operator[1]));
    }

    @Operation(summary = "批量执行月结")
    @PostMapping("/batch-execute")
    @PreAuthorize("hasPermission('/api/erp/finance/month-closing/execute', 'finance:month-closing:execute')")
    @OperationLog(module = "总账月结", type = "UPDATE", desc = "批量执行月结")
    public Result<List<MonthClosingResultDTO>> batchExecute(
            @Parameter(description = "期间编码列表 yyyy-MM") @RequestBody List<String> periodCodes,
            @Parameter(description = "操作人ID") @RequestHeader(value = "userId", required = false) String operatorId,
            @Parameter(description = "操作人姓名") @RequestHeader(value = "username", required = false) String operatorName) {
        String[] operator = resolveOperator(operatorId, operatorName);
        return Result.success(monthClosingService.batchExecute(periodCodes, operator[0], operator[1]));
    }

    @Operation(summary = "反月结（重新开启期间）")
    @PostMapping("/reopen")
    @PreAuthorize("hasPermission('/api/erp/finance/month-closing/reopen', 'finance:month-closing:reopen')")
    @OperationLog(module = "总账月结", type = "UPDATE", desc = "反月结")
    public Result<MonthClosingResultDTO> reopen(
            @Parameter(description = "期间编码 yyyy-MM") @RequestParam String periodCode,
            @Parameter(description = "操作人ID") @RequestHeader(value = "userId", required = false) String operatorId,
            @Parameter(description = "操作人姓名") @RequestHeader(value = "username", required = false) String operatorName) {
        String[] operator = resolveOperator(operatorId, operatorName);
        return Result.success(monthClosingService.reopen(periodCode, operator[0], operator[1]));
    }

    @Operation(summary = "查询期间月结状态")
    @GetMapping("/status")
    @PreAuthorize("hasPermission('/api/erp/finance/month-closing/status', 'finance:month-closing:view')")
    @OperationLog(module = "总账月结", type = "QUERY", desc = "查询期间月结状态")
    public Result<Map<String, Object>> status(
            @Parameter(description = "期间编码 yyyy-MM") @RequestParam String periodCode) {
        return Result.success(monthClosingService.status(periodCode));
    }

    @Operation(summary = "分页查询月结日志")
    @GetMapping("/logs/page")
    @PreAuthorize("hasPermission('/api/erp/finance/month-closing/logs', 'finance:month-closing:view')")
    @OperationLog(module = "总账月结", type = "QUERY", desc = "分页查询月结日志")
    public Result<IPage<MonthClosingLog>> logPage(
            @Parameter(description = "期间编码 yyyy-MM") @RequestParam(required = false) String periodCode,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        return Result.success(monthClosingService.logPage(periodCode, new Page<>(page, size)));
    }
}
