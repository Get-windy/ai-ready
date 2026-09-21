package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductUnitDict;
import cn.aiedge.erp.stock.service.ProductUnitDictService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 商品单位字典Controller
 *
 * <p>对标 ql361「商品辅助资料 → 商品单位」。
 */
@Slf4j
@Tag(name = "商品单位字典管理")
@RestController
@RequestMapping("/api/erp/product-unit-dict")
@RequiredArgsConstructor
public class ProductUnitDictController {

    private static final int EXPORT_MAX_ROWS = 20000;

    private static final String[][] EXPORT_COLUMNS = {
            {"unitName", "商品单位"},
            {"mnemonicCode", "助记码"},
            {"remark", "计量单位备注"},
            {"isDefault", "是否默认"},
    };

    private final ProductUnitDictService productUnitDictService;

    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    @Operation(summary = "分页查询单位列表")
    @SaCheckPermission("product:unit-dict:list")
    @GetMapping("/page")
    public Result<IPage<ProductUnitDict>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productUnitDictService.getPage(currentTenantId(), keyword, pageNum, pageSize));
    }

    @Operation(summary = "获取所有单位（当前租户）")
    @SaCheckPermission("product:unit-dict:list")
    @GetMapping("/list")
    public Result<List<ProductUnitDict>> list() {
        return Result.ok(productUnitDictService.getByTenantId(currentTenantId()));
    }

    @Operation(summary = "创建单位")
    @SaCheckPermission("product:unit-dict:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductUnitDict unit) {
        unit.setTenantId(currentTenantId());
        unit.setId(null);
        if (unit.getStatus() == null) unit.setStatus(1);
        if (unit.getSortOrder() == null) unit.setSortOrder(0);
        if (unit.getIsDefault() == null) unit.setIsDefault(0);
        return Result.ok(productUnitDictService.save(unit));
    }

    @Operation(summary = "更新单位")
    @SaCheckPermission("product:unit-dict:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductUnitDict unit) {
        unit.setId(id);
        return Result.ok(productUnitDictService.updateById(unit));
    }

    @Operation(summary = "删除单位")
    @SaCheckPermission("product:unit-dict:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productUnitDictService.removeById(id));
    }

    @Operation(summary = "导出单位（真实 Excel 流，与分页查询同一过滤口径）")
    @SaCheckPermission("product:unit-dict:export")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws IOException {
        IPage<ProductUnitDict> page = productUnitDictService.getPage(currentTenantId(), keyword, 1, EXPORT_MAX_ROWS);
        List<ProductUnitDict> rows = page.getRecords();
        if (rows.size() > EXPORT_MAX_ROWS) {
            rows = rows.subList(0, EXPORT_MAX_ROWS);
        }

        String fileName = "商品单位_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("商品单位");
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
            for (ProductUnitDict row : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(row.getUnitName() == null ? "" : row.getUnitName());
                excelRow.createCell(1).setCellValue(row.getMnemonicCode() == null ? "" : row.getMnemonicCode());
                excelRow.createCell(2).setCellValue(row.getRemark() == null ? "" : row.getRemark());
                excelRow.createCell(3).setCellValue(
                        row.getIsDefault() != null && row.getIsDefault() == 1 ? "是" : "否");
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}
