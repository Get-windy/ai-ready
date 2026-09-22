package cn.aiedge.erp.finance.controller;

import cn.aiedge.erp.finance.bankaccount.dto.BankAccountDTO;
import cn.aiedge.erp.finance.bankaccount.dto.BankAccountQueryDTO;
import cn.aiedge.erp.finance.bankaccount.service.BankAccountService;
import cn.aiedge.erp.finance.model.entity.FinanceAccount;
import cn.aiedge.erp.finance.service.FinanceAccountService;
import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 支付账户 / 财务账户 Controller（资料 → 支付管理 → 支付账户）
 *
 * 单一口径（红线）：资金账户数据一律以 {@code finance_account} 为准，
 *   读多条件分页/增删改/启停/编号建议均复用 {@link BankAccountService}（与《银行账户》同一 Service），
 *   本类不重复实现任何写业务逻辑，仅做「支付账户」视图的接口编排。
 *
 * ⚠️ 鉴权易错点：本系统鉴权走 sa-token（登录拦截器 + StpInterface）。
 *    请勿给本 Controller 方法加 Spring Security 的 @PreAuthorize —— sa-token 登录
 *    不会填充 Spring Security 的 SecurityContext，@PreAuthorize 恒抛
 *    AccessDeniedException，且 GlobalExceptionHandler 未专门处理它，会兜底返回 500。
 *    这里曾因此导致 /list 等接口恒 500（提存表单账户下拉取不到数据），已移除
 *    @PreAuthorize（接口仍受 sa-token 登录保护）。
 */
@Tag(name = "支付账户（资金账户）", description = "财务资金账户查询/增删改/启停/余额调整/导出")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/account")
@RequiredArgsConstructor
public class FinanceAccountController {

    private final FinanceAccountService financeAccountService;
    /** 资金账户唯一写入口（与《银行账户》共用，严禁在此重复实现 CRUD 业务规则） */
    private final BankAccountService bankAccountService;

    // ==================== 支付账户视图（分页 / CRUD） ====================

    /**
     * 分页查询支付账户（支持 关键字/账户类型/账户等级/状态/币种 过滤）
     */
    @Operation(summary = "分页查询支付账户")
    @SaCheckPermission("finance:account:list")
    @GetMapping("/page")
    @OperationLog(module = "支付账户", type = "QUERY", desc = "分页查询支付账户")
    public Result<Page<BankAccountDTO>> page(BankAccountQueryDTO query) {
        return Result.success(bankAccountService.page(query));
    }

    /**
     * 支付账户详情
     */
    @Operation(summary = "查询支付账户详情")
    @SaCheckPermission("finance:account:detail")
    @GetMapping("/{id}")
    @OperationLog(module = "支付账户", type = "QUERY", desc = "查询支付账户详情")
    public Result<BankAccountDTO> detail(@PathVariable Long id) {
        return Result.success(bankAccountService.getById(id));
    }

    /**
     * 生成下一个账户编号（对标「银行编号」自动建议）
     */
    @Operation(summary = "生成下一个账户编号")
    @SaCheckPermission("finance:account:view")
    @GetMapping("/next-code")
    @OperationLog(module = "支付账户", type = "QUERY", desc = "生成下一个账户编号")
    public Result<String> nextCode(
            @Parameter(description = "上级账户ID（不传为顶级编号）") @RequestParam(required = false) Long parentId) {
        return Result.success(bankAccountService.nextCode(parentId));
    }

    /**
     * 新增支付账户（复用《银行账户》写入口）
     */
    @Operation(summary = "新增支付账户")
    @SaCheckPermission("finance:account:create")
    @PostMapping
    @OperationLog(module = "支付账户", type = "CREATE", desc = "新增支付账户")
    public Result<BankAccountDTO> create(@RequestBody BankAccountDTO dto) {
        return Result.success("新增成功", bankAccountService.create(dto));
    }

    /**
     * 修改支付账户（复用《银行账户》写入口）
     */
    @Operation(summary = "修改支付账户")
    @SaCheckPermission("finance:account:update")
    @PutMapping("/{id}")
    @OperationLog(module = "支付账户", type = "UPDATE", desc = "修改支付账户")
    public Result<BankAccountDTO> update(@PathVariable Long id, @RequestBody BankAccountDTO dto) {
        return Result.success("修改成功", bankAccountService.update(id, dto));
    }

    /**
     * 删除支付账户（复用《银行账户》写入口：预置账户/有下级/余额非0 均拒绝）
     */
    @Operation(summary = "删除支付账户")
    @SaCheckPermission("finance:account:delete")
    @DeleteMapping("/{id}")
    @OperationLog(module = "支付账户", type = "DELETE", desc = "删除支付账户")
    public Result<Void> delete(@PathVariable Long id) {
        bankAccountService.delete(id);
        return Result.success("删除成功", null);
    }

    // ==================== 兼容既有口径（下拉 / 统计 / 启停 / 余额） ====================

    /**
     * 查询财务账户列表（不分页）
     * ⚠️ 被全平台账户下拉引用（提存现/费用单/收付款/options.ts），签名与行为保持兼容，勿改。
     */
    @OperationLog(module = "财务账户管理", type = "QUERY", desc = "查询财务账户列表")
    @SaCheckPermission("finance:account:list")
    @GetMapping("/list")
    public Result<List<FinanceAccount>> listAccounts(
            @Parameter(description = "账户状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "账户类型") @RequestParam(required = false) String accountType) {
        List<FinanceAccount> accounts = financeAccountService.listAccounts(status, accountType);
        return Result.success(accounts);
    }

    /**
     * 查询账户统计信息（账户总数 / 启用数 / 停用数 / 余额合计）
     * 支持与列表同口径过滤，统计卡片走后端，前端不再本地汇总。
     */
    @Operation(summary = "查询支付账户统计")
    @OperationLog(module = "支付账户", type = "QUERY", desc = "查询支付账户统计")
    @SaCheckPermission("finance:account:view")
    @GetMapping("/statistics")
    public Result<Object> getAccountStatistics(
            @Parameter(description = "账户状态 0-停用 1-启用") @RequestParam(required = false) Integer status,
            @Parameter(description = "账户类型 1-银行 2-现金 3-内部 4-外部") @RequestParam(required = false) Integer accountType,
            @Parameter(description = "账户等级 1-基本 2-一般 3-专用") @RequestParam(required = false) Integer accountLevel,
            @Parameter(description = "币种") @RequestParam(required = false) String currency,
            @Parameter(description = "账户名称/开户银行/银行账号/科目编号") @RequestParam(required = false) String keyword) {
        return Result.success(financeAccountService.getAccountStatistics(status, accountType, accountLevel, currency, keyword));
    }

    /**
     * 启用/停用账户
     */
    @OperationLog(module = "财务账户管理", type = "UPDATE", desc = "更新账户状态")
    @SaCheckPermission("finance:account:update")
    @PutMapping("/status/{id}")
    public Result<Void> updateAccountStatus(
            @Parameter(description = "账户ID") @PathVariable Long id,
            @Parameter(description = "状态(0-停用,1-启用)") @RequestBody Integer status) {
        BankAccountDTO updated = bankAccountService.updateStatus(id, status);
        return updated != null ? Result.success() : Result.error("更新失败");
    }

    /**
     * 余额调整（增量口径：正数增加、负数减少）
     */
    @OperationLog(module = "财务账户管理", type = "UPDATE", desc = "更新账户余额")
    @SaCheckPermission("finance:account:update")
    @PutMapping("/balance/{accountId}")
    public Result<Void> updateAccountBalance(
            @Parameter(description = "账户ID") @PathVariable Long accountId,
            @Parameter(description = "调整金额(正数增加/负数减少)") @RequestBody BigDecimal amount) {
        boolean success = financeAccountService.updateAccountBalance(accountId, amount);
        return success ? Result.success() : Result.error("更新失败");
    }

    // ==================== 导出 ====================

    /**
     * 导出支付账户（真实 xlsx，按当前查询条件）
     */
    @Operation(summary = "导出支付账户")
    @SaCheckPermission("finance:account:export")
    @GetMapping("/export")
    @OperationLog(module = "支付账户", type = "QUERY", desc = "导出支付账户")
    public void export(BankAccountQueryDTO query, HttpServletResponse response) throws IOException {
        List<BankAccountDTO> rows = bankAccountService.list(query);

        String fileName = "支付账户_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {"账户名称", "科目编号", "账户类型", "账户等级", "开户银行", "银行账号",
                "账户余额", "币种", "状态", "备注"};

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("支付账户");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }

            int rowIdx = 1;
            for (BankAccountDTO dto : rows) {
                Row row = sheet.createRow(rowIdx++);
                String[] values = {
                        nullSafe(dto.getAccountName()),
                        nullSafe(dto.getSubjectCode()),
                        accountTypeText(dto.getAccountType()),
                        accountLevelText(dto.getAccountLevel()),
                        nullSafe(dto.getBankName()),
                        nullSafe(dto.getBankAccount()),
                        dto.getBalance() == null ? "0.00" : dto.getBalance().toPlainString(),
                        nullSafe(dto.getCurrency()),
                        (dto.getStatus() != null && dto.getStatus() == 1) ? "启用" : "停用",
                        nullSafe(dto.getRemark()),
                };
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    /** 账户类型字典：1-银行账户 2-现金账户 3-内部账户 4-外部账户（与 FinanceAccount 注释一致） */
    private String accountTypeText(Integer type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case 1 -> "银行账户";
            case 2 -> "现金账户";
            case 3 -> "内部账户";
            case 4 -> "外部账户";
            default -> "";
        };
    }

    /** 账户等级字典：1-基本账户 2-一般账户 3-专用账户 */
    private String accountLevelText(Integer level) {
        if (level == null) {
            return "";
        }
        return switch (level) {
            case 1 -> "基本账户";
            case 2 -> "一般账户";
            case 3 -> "专用账户";
            default -> "";
        };
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
