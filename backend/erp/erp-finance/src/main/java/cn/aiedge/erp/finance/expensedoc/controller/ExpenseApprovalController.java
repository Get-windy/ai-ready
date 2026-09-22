package cn.aiedge.erp.finance.expensedoc.controller;

import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.service.SysUserService;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.expensedoc.dto.ApprovalProcessDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ApprovalSubmitDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApprovalDetailVO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApprovalQuery;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApproverConfigDTO;
import cn.aiedge.erp.finance.expensedoc.dto.ExpenseApproverSuggestionVO;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseApproval;
import cn.aiedge.erp.finance.expensedoc.entity.ExpenseDoc;
import cn.aiedge.erp.finance.expensedoc.service.ExpenseApprovalService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 费用审批 Controller
 * 对《费用单》(erp_expense_doc) 的多级审批：部门 → 财务 → 总经理，审批通过后方可记账。
 * 审批状态复用费用单主表（P0 单一口径），过程留痕落 erp_expense_approval。
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/expense-approval")
@RequiredArgsConstructor
@Tag(name = "费用审批管理", description = "费用单多级审批：待审批工作台、提交审批、通过/驳回、审批记录")
public class ExpenseApprovalController {

    private final ExpenseApprovalService expenseApprovalService;
    private final SysUserService sysUserService;

    @Operation(summary = "待审批分页查询", description = "默认只查审批中的费用单；onlyMine=true 时按当前登录人过滤（审批人隔离）")
    @SaCheckPermission("finance:expense-approval:view")
    @GetMapping("/pending")
    public Result<Page<ExpenseDoc>> pending(ExpenseApprovalQuery query) {
        return Result.ok(expenseApprovalService.pagePending(query, currentUserId()));
    }

    @Operation(summary = "提交审批", description = "草稿/已驳回的费用单提交后进入第一级审批队列")
    @SaCheckPermission("finance:expense-approval:submit")
    @PostMapping("/submit")
    public Result<ExpenseDoc> submit(@Valid @RequestBody ApprovalSubmitDTO dto) {
        Long operatorId = currentUserId();
        return Result.ok(expenseApprovalService.submit(dto, operatorId, resolveUserName(operatorId)));
    }

    @Operation(summary = "审批处理", description = "通过（未到末级则流转下一级）/ 驳回（原因必填）")
    @SaCheckPermission("finance:expense-approval:create")
    @PostMapping("/process")
    public Result<ExpenseDoc> process(@Valid @RequestBody ApprovalProcessDTO dto) {
        Long operatorId = currentUserId();
        return Result.ok(expenseApprovalService.process(dto, operatorId, resolveUserName(operatorId)));
    }

    @Operation(summary = "审批记录", description = "按费用单查询审批留痕（提交/通过/驳回）")
    @SaCheckPermission("finance:expense-approval:view")
    @GetMapping("/records")
    public Result<List<ExpenseApproval>> records(@Parameter(description = "费用单ID") @RequestParam Long docId) {
        return Result.ok(expenseApprovalService.records(docId));
    }

    @Operation(summary = "审批详情", description = "费用单（含费用项明细）+ 审批记录")
    @SaCheckPermission("finance:expense-approval:detail")
    @GetMapping("/detail/{docId}")
    public Result<ExpenseApprovalDetailVO> detail(@PathVariable Long docId) {
        return Result.ok(expenseApprovalService.detail(docId));
    }

    @Operation(summary = "审批人配置查询", description = "按级别返回默认审批人（用于自动指派）")
    @SaCheckPermission("finance:expense-approval:view")
    @GetMapping("/approver-config")
    public Result<ExpenseApproverConfigDTO> getApproverConfig() {
        return Result.ok(expenseApprovalService.getApproverConfig());
    }

    @Operation(summary = "审批人配置保存", description = "按级别保存默认审批人（null 表示清除该级别）")
    @SaCheckPermission("finance:expense-approval:create")
    @PostMapping("/approver-config")
    public Result<String> saveApproverConfig(@RequestBody ExpenseApproverConfigDTO dto) {
        expenseApprovalService.saveApproverConfig(dto);
        return Result.ok("保存成功");
    }

    @Operation(summary = "审批人自动指派建议",
            description = "按级别返回建议审批人：一级优先取费用单部门负责人，未设置时取审批人配置；二/三级取审批人配置")
    @SaCheckPermission("finance:expense-approval:view")
    @GetMapping("/suggest")
    public Result<List<ExpenseApproverSuggestionVO>> suggest(
            @Parameter(description = "费用单ID（用于解析部门负责人）") @RequestParam(required = false) Long docId,
            @Parameter(description = "审批总级数") @RequestParam(required = false) Integer totalLevel) {
        return Result.ok(expenseApprovalService.suggestApprovers(docId, totalLevel));
    }

    // ── 当前登录人 ──

    private Long currentUserId() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (Exception e) {
            log.warn("获取当前登录人失败: {}", e.getMessage());
            return null;
        }
    }

    /** 审批人展示名：优先昵称/真实姓名，退化到用户名 */
    private String resolveUserName(Long userId) {
        if (userId == null) {
            return "系统";
        }
        try {
            SysUser user = sysUserService.getById(userId);
            if (user != null) {
                if (user.getNickname() != null && !user.getNickname().isEmpty()) {
                    return user.getNickname();
                }
                if (user.getRealName() != null && !user.getRealName().isEmpty()) {
                    return user.getRealName();
                }
                if (user.getUsername() != null && !user.getUsername().isEmpty()) {
                    return user.getUsername();
                }
            }
        } catch (Exception e) {
            log.warn("查询审批人名称失败: userId={}, error={}", userId, e.getMessage());
        }
        return String.valueOf(userId);
    }
}
