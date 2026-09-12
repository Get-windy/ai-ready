package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.dto.AccountSubjectQuery;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import cn.aiedge.erp.finance.service.AccountSubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.annotation.SaCheckPermission;
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
 * 会计科目Controller
 */
@Tag(name = "会计科目管理", description = "会计科目CRUD及树形结构接口")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/subject")
@RequiredArgsConstructor
public class AccountSubjectController {

    /** 导出列：[字段, 表头] */
    private static final String[][] EXPORT_COLUMNS = {
            {"subjectCode", "科目编号"},
            {"subjectName", "科目名称"},
            {"auxiliaryTypeName", "核算项"}
    };

    private final AccountSubjectService accountSubjectService;

    @Operation(summary = "查询会计科目列表（支持 科目名称/编号/助记码、科目分类、显示停用）")
    @GetMapping("/list")
    @SaCheckPermission("finance:subject:view")
    @OperationLog(module = "会计科目管理", type = "QUERY", desc = "查询会计科目列表")
    public Result<List<AccountSubjectDTO>> list(AccountSubjectQuery query) {
        return Result.success(accountSubjectService.search(query));
    }

    @Operation(summary = "查询会计科目树形结构（hierarchical=false 时返回平铺列表）")
    @GetMapping("/tree")
    @SaCheckPermission("finance:subject:view")
    @OperationLog(module = "会计科目管理", type = "QUERY", desc = "查询会计科目树形结构")
    public Result<List<AccountSubjectDTO>> tree(AccountSubjectQuery query) {
        return Result.success(accountSubjectService.getTree(query));
    }

    @Operation(summary = "根据ID查询会计科目")
    @GetMapping("/{id}")
    @SaCheckPermission("finance:subject:view")
    @OperationLog(module = "会计科目管理", type = "QUERY", desc = "根据ID查询会计科目")
    public Result<AccountSubjectDTO> getById(@Parameter(description = "科目ID") @PathVariable Long id) {
        return Result.success(accountSubjectService.getById(id));
    }

    @Operation(summary = "核算项可选项（辅助核算类型，供科目编辑器下拉）")
    @GetMapping("/aux-types")
    @SaCheckPermission("finance:subject:view")
    public Result<List<FinanceAuxiliaryTypeDTO>> auxTypes() {
        return Result.success(accountSubjectService.getAuxTypeOptions());
    }

    @Operation(summary = "根据科目类型查询")
    @GetMapping("/type/{subjectType}")
    @SaCheckPermission("finance:subject:view")
    @OperationLog(module = "会计科目管理", type = "QUERY", desc = "根据科目类型查询")
    public Result<List<AccountSubjectDTO>> getByType(@Parameter(description = "科目类型(1-资产,2-负债,3-权益,4-成本,5-损益)") @PathVariable Integer subjectType) {
        return Result.success(accountSubjectService.getByType(subjectType));
    }

    @Operation(summary = "创建会计科目")
    // 同时匹配 /api/erp/finance/subject 与带尾斜杠两种写法（前端调用无尾斜杠，Spring Boot 3 不再自动重定向）
    @PostMapping({"", "/"})
    @SaCheckPermission("finance:subject:create")
    @OperationLog(module = "会计科目管理", type = "CREATE", desc = "创建会计科目")
    public Result<AccountSubjectDTO> create(@RequestBody AccountSubjectDTO dto) {
        AccountSubjectDTO result = accountSubjectService.create(dto);
        return Result.success("创建成功", result);
    }

    @Operation(summary = "更新会计科目")
    @PutMapping("/{id}")
    @SaCheckPermission("finance:subject:edit")
    @OperationLog(module = "会计科目管理", type = "UPDATE", desc = "更新会计科目")
    public Result<AccountSubjectDTO> update(
            @Parameter(description = "科目ID") @PathVariable Long id,
            @RequestBody AccountSubjectDTO dto) {
        AccountSubjectDTO result = accountSubjectService.update(id, dto);
        return Result.success("更新成功", result);
    }

    @Operation(summary = "删除会计科目")
    @DeleteMapping("/{id}")
    @SaCheckPermission("finance:subject:delete")
    @OperationLog(module = "会计科目管理", type = "DELETE", desc = "删除会计科目")
    public Result<Void> delete(@Parameter(description = "科目ID") @PathVariable Long id) {
        accountSubjectService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "启用/停用会计科目")
    @PutMapping("/{id}/enable")
    @SaCheckPermission("finance:subject:edit")
    @OperationLog(module = "会计科目管理", type = "UPDATE", desc = "启用/停用会计科目")
    public Result<AccountSubjectDTO> toggleEnabled(
            @Parameter(description = "科目ID") @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam boolean enabled) {
        AccountSubjectDTO result = accountSubjectService.enable(id, enabled);
        return Result.success("操作成功", result);
    }

    @Operation(summary = "批量删除会计科目")
    @DeleteMapping("/batch")
    @SaCheckPermission("finance:subject:delete")
    @OperationLog(module = "会计科目管理", type = "DELETE", desc = "批量删除会计科目")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        accountSubjectService.deleteBatch(ids);
        return Result.success("批量删除成功", null);
    }

    @Operation(summary = "导出会计科目（真实 Excel 流，与列表同一过滤口径）")
    @GetMapping("/export")
    @SaCheckPermission("finance:subject:view")
    public void export(AccountSubjectQuery query, HttpServletResponse response) throws IOException {
        List<AccountSubjectDTO> rows = accountSubjectService.search(query);

        String fileName = "会计科目_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("会计科目");
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
                sheet.setColumnWidth(i, 22 * 256);
            }
            workbook.write(response.getOutputStream());
        }
        log.info("导出会计科目: rows={}", rows.size());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
