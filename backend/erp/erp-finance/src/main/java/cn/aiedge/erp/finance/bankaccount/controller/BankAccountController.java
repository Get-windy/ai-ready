package cn.aiedge.erp.finance.bankaccount.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.finance.bankaccount.dto.BankAccountDTO;
import cn.aiedge.erp.finance.bankaccount.dto.BankAccountQueryDTO;
import cn.aiedge.erp.finance.bankaccount.service.BankAccountService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * 银行账户（资料 → 财务账户 → 银行账户）Controller
 *
 * 对标 ql361：新增银行 / 刷新 / 打印(F8) / 导出 / 筛选条件 + 显示停用 + 显示层次结构 / 树形列表 / 行内 修改·删除·更多。
 * 数据单一口径复用 finance_account（与《支付账户》同源），严禁另建重复银行账户表。
 *
 * ⚠️ 鉴权口径同 FinanceAccountController：走 sa-token 登录拦截，勿加 Spring Security @PreAuthorize。
 */
@Tag(name = "银行账户管理", description = "银行账户（资金账户）增删改查、树形分页、导出")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/bank-account")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    /** 账户类型字典：1-银行账户 2-现金账户 3-内部账户 4-外部账户（与 FinanceAccount.accountType 注释一致） */
    private static final String[] ACCOUNT_TYPE_TEXT = {"", "银行账户", "现金账户", "内部账户", "外部账户"};

    @Operation(summary = "分页查询银行账户（支持显示停用/显示层次结构）")
    @GetMapping("/page")
    @OperationLog(module = "银行账户", type = "QUERY", desc = "分页查询银行账户")
    public ApiResponse<Page<BankAccountDTO>> page(BankAccountQueryDTO query) {
        return ApiResponse.success(bankAccountService.page(query));
    }

    @Operation(summary = "查询银行账户详情")
    @GetMapping("/{id}")
    @OperationLog(module = "银行账户", type = "QUERY", desc = "查询银行账户详情")
    public ApiResponse<BankAccountDTO> detail(@PathVariable Long id) {
        return ApiResponse.success(bankAccountService.getById(id));
    }

    @Operation(summary = "上级账户下拉（树形顺序）")
    @GetMapping("/options")
    @OperationLog(module = "银行账户", type = "QUERY", desc = "查询银行账户下拉")
    public ApiResponse<List<BankAccountDTO>> options() {
        return ApiResponse.success(bankAccountService.options());
    }

    @Operation(summary = "生成下一个银行编号")
    @GetMapping("/next-code")
    @OperationLog(module = "银行账户", type = "QUERY", desc = "生成下一个银行编号")
    public ApiResponse<String> nextCode(
            @Parameter(description = "上级账户ID（不传为顶级编号）") @RequestParam(required = false) Long parentId) {
        return ApiResponse.success(bankAccountService.nextCode(parentId));
    }

    @Operation(summary = "新增银行账户")
    @PostMapping
    @OperationLog(module = "银行账户", type = "CREATE", desc = "新增银行账户")
    public ApiResponse<BankAccountDTO> create(@RequestBody BankAccountDTO dto) {
        return ApiResponse.success("新增成功", bankAccountService.create(dto));
    }

    @Operation(summary = "修改银行账户")
    @PutMapping("/{id}")
    @OperationLog(module = "银行账户", type = "UPDATE", desc = "修改银行账户")
    public ApiResponse<BankAccountDTO> update(@PathVariable Long id, @RequestBody BankAccountDTO dto) {
        return ApiResponse.success("修改成功", bankAccountService.update(id, dto));
    }

    @Operation(summary = "删除银行账户")
    @DeleteMapping("/{id}")
    @OperationLog(module = "银行账户", type = "DELETE", desc = "删除银行账户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bankAccountService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "启用/停用银行账户")
    @PutMapping("/{id}/status")
    @OperationLog(module = "银行账户", type = "UPDATE", desc = "启用/停用银行账户")
    public ApiResponse<BankAccountDTO> updateStatus(
            @PathVariable Long id,
            @Parameter(description = "状态(0-停用,1-启用)") @RequestBody Integer status) {
        return ApiResponse.success(bankAccountService.updateStatus(id, status));
    }

    @Operation(summary = "导出银行账户（真实 xlsx）")
    @GetMapping("/export")
    @OperationLog(module = "银行账户", type = "QUERY", desc = "导出银行账户")
    public void export(BankAccountQueryDTO query, HttpServletResponse response) throws IOException {
        List<BankAccountDTO> rows = bankAccountService.list(query);

        String fileName = "银行账户_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[][] columns = {
                {"科目编号", "subjectCode"},
                {"科目名称", "accountName"},
                {"账户类型", "accountTypeText"},
                {"是否用于商城线下转账收款", "mallTransferText"},
                {"助记码", "easyCode"},
                {"银行简称", "briefName"},
                {"开户行", "bankName"},
                {"户主名", "accountHolder"},
                {"银行账号", "bankAccount"},
                {"币种", "currency"},
                {"账户余额", "balanceText"},
                {"状态", "statusText"},
                {"备注", "remark"},
        };

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("银行账户");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(columns[i][0]);
                cell.setCellStyle(headStyle);
            }

            int rowIdx = 1;
            for (BankAccountDTO dto : rows) {
                Row row = sheet.createRow(rowIdx++);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(readCell(dto, columns[i][1]));
                }
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    /** 导出单元格取值（含字典翻译与空值保护） */
    private String readCell(BankAccountDTO dto, String field) {
        Function<BankAccountDTO, String> reader = switch (field) {
            case "subjectCode" -> d -> nullSafe(d.getSubjectCode());
            case "accountName" -> d -> nullSafe(d.getAccountName());
            case "accountTypeText" -> d -> accountTypeText(d.getAccountType());
            case "mallTransferText" -> d -> (d.getMallTransferEnabled() != null && d.getMallTransferEnabled() == 1) ? "是" : "否";
            case "easyCode" -> d -> nullSafe(d.getEasyCode());
            case "briefName" -> d -> nullSafe(d.getBriefName());
            case "bankName" -> d -> nullSafe(d.getBankName());
            case "accountHolder" -> d -> nullSafe(d.getAccountHolder());
            case "bankAccount" -> d -> nullSafe(d.getBankAccount());
            case "currency" -> d -> nullSafe(d.getCurrency());
            case "balanceText" -> d -> d.getBalance() == null ? "0.00" : d.getBalance().toPlainString();
            case "statusText" -> d -> (d.getStatus() != null && d.getStatus() == 1) ? "启用" : "停用";
            case "remark" -> d -> nullSafe(d.getRemark());
            default -> d -> "";
        };
        return reader.apply(dto);
    }

    private String accountTypeText(Integer type) {
        if (type == null || type < 1 || type >= ACCOUNT_TYPE_TEXT.length) {
            return "";
        }
        return ACCOUNT_TYPE_TEXT[type];
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /** 保留：余额格式化统一入口（前端金额展示口径与服务端一致） */
    static String formatAmount(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
