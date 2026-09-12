package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.AccountSubjectDTO;
import cn.aiedge.erp.finance.dto.AccountSubjectQuery;
import cn.aiedge.erp.finance.dto.FinanceAuxiliaryTypeDTO;
import cn.aiedge.erp.finance.service.OtherIncomeSubjectService;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * 其他收入（资料 → 财务账户 → 其他收入）Controller。
 *
 * <p>对标 ql361：单入口平铺列表；无左侧分类树、无页面配置弹窗；列配置齿轮在数据表表头。
 * 查询区仅「显示停用」；列 3 个（科目编号/科目名称/核算项）；行内 修改/删除/更多；
 * 编辑弹窗即「会计科目」完整编辑器（科目编号/科目名称/助记码/科目全名/核算项/借-贷）。</p>
 *
 * <p>⚠️ 本页数据源是收入类会计科目（finance_account_subject，subject_type=5 且 direction=2），
 * 与《其他收入单》（/api/erp/finance/other-income-doc，fin_other_income_doc）严格区分：
 * 前者是科目主数据视图，后者是收入单据。</p>
 */
@Tag(name = "其他收入", description = "收入类会计科目视图（资料 → 财务账户 → 其他收入）")
@Slf4j
@RestController
@RequestMapping("/api/erp/md/other-income")
@RequiredArgsConstructor
public class OtherIncomeSubjectController {

    private final OtherIncomeSubjectService otherIncomeSubjectService;

    @Operation(summary = "分页查询收入科目")
    @GetMapping("/page")
    @OperationLog(module = "其他收入", type = "QUERY", desc = "分页查询收入科目")
    public Result<Page<AccountSubjectDTO>> page(
            AccountSubjectQuery query,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(otherIncomeSubjectService.page(query, pageNum, pageSize));
    }

    @Operation(summary = "收入科目平铺列表（下拉/导出共用口径）")
    @GetMapping("/list")
    @OperationLog(module = "其他收入", type = "QUERY", desc = "查询收入科目列表")
    public Result<List<AccountSubjectDTO>> list(AccountSubjectQuery query) {
        return Result.success(otherIncomeSubjectService.list(query));
    }

    @Operation(summary = "核算项可选项（辅助核算类型，供收入科目编辑器下拉）")
    @GetMapping("/aux-types")
    public Result<List<FinanceAuxiliaryTypeDTO>> auxTypes() {
        return Result.success(otherIncomeSubjectService.getAuxTypeOptions());
    }

    @Operation(summary = "收入科目详情")
    @GetMapping("/{id}")
    @OperationLog(module = "其他收入", type = "QUERY", desc = "查询收入科目详情")
    public Result<AccountSubjectDTO> getById(@Parameter(description = "科目ID") @PathVariable Long id) {
        return Result.success(otherIncomeSubjectService.getById(id));
    }

    @Operation(summary = "新增收入")
    @PostMapping({"", "/"})
    @OperationLog(module = "其他收入", type = "CREATE", desc = "新增收入科目")
    public Result<AccountSubjectDTO> create(@RequestBody AccountSubjectDTO dto) {
        return Result.success("新增成功", otherIncomeSubjectService.create(dto));
    }

    @Operation(summary = "修改收入")
    @PutMapping("/{id}")
    @OperationLog(module = "其他收入", type = "UPDATE", desc = "修改收入科目")
    public Result<AccountSubjectDTO> update(
            @Parameter(description = "科目ID") @PathVariable Long id,
            @RequestBody AccountSubjectDTO dto) {
        return Result.success("修改成功", otherIncomeSubjectService.update(id, dto));
    }

    @Operation(summary = "删除收入")
    @DeleteMapping("/{id}")
    @OperationLog(module = "其他收入", type = "DELETE", desc = "删除收入科目")
    public Result<Void> delete(@Parameter(description = "科目ID") @PathVariable Long id) {
        otherIncomeSubjectService.delete(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "启用/停用收入科目")
    @PutMapping("/{id}/enable")
    @OperationLog(module = "其他收入", type = "UPDATE", desc = "启用/停用收入科目")
    public Result<AccountSubjectDTO> toggleEnabled(
            @Parameter(description = "科目ID") @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam boolean enabled) {
        return Result.success("操作成功", otherIncomeSubjectService.toggleEnabled(id, enabled));
    }

    @Operation(summary = "导出收入科目（真实 Excel 流，与列表同一口径）")
    @GetMapping("/export")
    public void export(AccountSubjectQuery query, HttpServletResponse response) throws IOException {
        List<AccountSubjectDTO> rows = otherIncomeSubjectService.list(query);

        String fileName = "其他收入_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("其他收入");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            String[] headers = {"科目编号", "科目名称", "核算项"};
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }
            int rowIdx = 1;
            for (AccountSubjectDTO row : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(nullToEmpty(row.getSubjectCode()));
                excelRow.createCell(1).setCellValue(nullToEmpty(row.getSubjectName()));
                excelRow.createCell(2).setCellValue(nullToEmpty(row.getAuxiliaryTypeName()));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 24 * 256);
            }
            workbook.write(response.getOutputStream());
        }
        log.info("导出其他收入: rows={}", rows.size());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
