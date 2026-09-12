package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.dto.AccountSubjectQuery;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import cn.aiedge.erp.finance.service.ExpenseTypeSubjectService;
import cn.aiedge.erp.finance.support.AccountSubjectTreeBuilder;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 费用类型（资料 → 财务账户 → 费用类型）Controller。
 *
 * <p>对标 ql361 实测：本页与「会计科目」页是同一数据源的两个视图——
 * 两页调用同一接口 {@code cc.erp.bll.bas.account.getlist}，仅 {@code bastype} 不同
 * （费用类型=fee/root=00004，会计科目=account/root=00000）；行内「修改」打开的编辑器
 * 标题为「会计科目」，字段为 科目编号/科目名称/助记码/科目全名/核算项/借-贷。</p>
 *
 * <p>⚠️ 本页数据源是费用类会计科目（finance_account_subject，subject_type=5 且 direction=1），
 * 与《费用单》（/api/erp/finance/expense-doc，erp_expense_doc）严格区分：
 * 前者是科目主数据视图，后者是费用单据。严禁另建费用类型字典表。</p>
 */
@Tag(name = "费用类型", description = "费用类会计科目视图（资料 → 财务账户 → 费用类型）")
@Slf4j
@RestController
@RequestMapping("/api/erp/md/expense-type")
@RequiredArgsConstructor
public class ExpenseTypeSubjectController {

    /** 导出列：[字段, 表头] */
    private static final String[][] EXPORT_COLUMNS = {
            {"subjectCode", "科目编号"},
            {"subjectName", "科目名称"},
            {"auxiliaryTypeName", "核算项"}
    };

    private final ExpenseTypeSubjectService expenseTypeSubjectService;

    @Operation(summary = "费用类科目树（hierarchical=false 时返回平铺列表）")
    @GetMapping("/tree")
    @OperationLog(module = "费用类型", type = "QUERY", desc = "查询费用类型树")
    public Result<List<AccountSubjectDTO>> tree(AccountSubjectQuery query) {
        List<AccountSubjectDTO> flat = expenseTypeSubjectService.list(query);
        if (query != null && !query.hierarchicalOrDefault()) {
            return Result.success(flat);
        }
        return Result.success(AccountSubjectTreeBuilder.build(flat));
    }

    @Operation(summary = "费用类科目平铺列表（下拉/导出共用口径）")
    @GetMapping("/list")
    @OperationLog(module = "费用类型", type = "QUERY", desc = "查询费用类型列表")
    public Result<List<AccountSubjectDTO>> list(AccountSubjectQuery query) {
        return Result.success(expenseTypeSubjectService.list(query));
    }

    @Operation(summary = "核算项可选项（辅助核算类型，供费用类型编辑器下拉）")
    @GetMapping("/aux-types")
    public Result<List<FinanceAuxiliaryTypeDTO>> auxTypes() {
        return Result.success(expenseTypeSubjectService.getAuxTypeOptions());
    }

    @Operation(summary = "费用类型详情")
    @GetMapping("/{id}")
    @OperationLog(module = "费用类型", type = "QUERY", desc = "查询费用类型详情")
    public Result<AccountSubjectDTO> getById(@Parameter(description = "科目ID") @PathVariable Long id) {
        return Result.success(expenseTypeSubjectService.getById(id));
    }

    @Operation(summary = "新增费用")
    @PostMapping({"", "/"})
    @OperationLog(module = "费用类型", type = "CREATE", desc = "新增费用科目")
    public Result<AccountSubjectDTO> create(@RequestBody AccountSubjectDTO dto) {
        return Result.success("新增成功", expenseTypeSubjectService.create(dto));
    }

    @Operation(summary = "修改费用")
    @PutMapping("/{id}")
    @OperationLog(module = "费用类型", type = "UPDATE", desc = "修改费用科目")
    public Result<AccountSubjectDTO> update(
            @Parameter(description = "科目ID") @PathVariable Long id,
            @RequestBody AccountSubjectDTO dto) {
        return Result.success("修改成功", expenseTypeSubjectService.update(id, dto));
    }

    @Operation(summary = "删除费用")
    @DeleteMapping("/{id}")
    @OperationLog(module = "费用类型", type = "DELETE", desc = "删除费用科目")
    public Result<Void> delete(@Parameter(description = "科目ID") @PathVariable Long id) {
        expenseTypeSubjectService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "启用/停用费用科目")
    @PutMapping("/{id}/enable")
    @OperationLog(module = "费用类型", type = "UPDATE", desc = "启用/停用费用科目")
    public Result<AccountSubjectDTO> toggleEnabled(
            @Parameter(description = "科目ID") @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam boolean enabled) {
        return Result.success("操作成功", expenseTypeSubjectService.toggleEnabled(id, enabled));
    }

    @Operation(summary = "导出费用类型（真实 Excel 流，与列表同一口径）")
    @GetMapping("/export")
    public void export(AccountSubjectQuery query, HttpServletResponse response) throws IOException {
        List<AccountSubjectDTO> rows = expenseTypeSubjectService.list(query);

        String fileName = "费用类型_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("费用类型");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(EXPORT_COLUMNS[i][1]);
                cell.setCellStyle(headStyle);
            }
            int rowIdx = 1;
            for (AccountSubjectDTO row : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(nullToEmpty(row.getSubjectCode()));
                excelRow.createCell(1).setCellValue(nullToEmpty(row.getSubjectName()));
                excelRow.createCell(2).setCellValue(nullToEmpty(row.getAuxiliaryTypeName()));
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sheet.setColumnWidth(i, 24 * 256);
            }
            workbook.write(response.getOutputStream());
        }
        log.info("导出费用类型: rows={}", rows.size());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
