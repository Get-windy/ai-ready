package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductBrand;
import cn.aiedge.erp.stock.service.ProductBrandService;
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

/**
 * 商品品牌Controller
 *
 * <p>对标 ql361「商品辅助资料 → 商品品牌」。
 */
@Slf4j
@Tag(name = "商品品牌管理")
@RestController
@RequestMapping("/api/erp/product-brand")
@RequiredArgsConstructor
public class ProductBrandController {

    private static final int EXPORT_MAX_ROWS = 20000;

    private static final String[][] EXPORT_COLUMNS = {
            {"brandName", "品牌名称"},
            {"mnemonicCode", "助记码"},
            {"remark", "备注"},
    };

    private final ProductBrandService productBrandService;

    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    @Operation(summary = "分页查询品牌列表")
    @GetMapping("/page")
    public Result<IPage<ProductBrand>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productBrandService.getPage(currentTenantId(), keyword, pageNum, pageSize));
    }

    @Operation(summary = "获取所有品牌（当前租户）")
    @GetMapping("/list")
    public Result<List<ProductBrand>> list() {
        return Result.ok(productBrandService.getByTenantId(currentTenantId()));
    }

    @Operation(summary = "创建品牌")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductBrand brand) {
        brand.setTenantId(currentTenantId());
        brand.setId(null);
        if (brand.getStatus() == null) brand.setStatus(1);
        if (brand.getSortOrder() == null) brand.setSortOrder(0);
        return Result.ok(productBrandService.save(brand));
    }

    @Operation(summary = "更新品牌")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductBrand brand) {
        brand.setId(id);
        return Result.ok(productBrandService.updateById(brand));
    }

    @Operation(summary = "删除品牌")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productBrandService.removeById(id));
    }

    @Operation(summary = "导出品牌（真实 Excel 流，与分页查询同一过滤口径）")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws IOException {
        IPage<ProductBrand> page = productBrandService.getPage(currentTenantId(), keyword, 1, EXPORT_MAX_ROWS);
        List<ProductBrand> rows = page.getRecords();
        if (rows.size() > EXPORT_MAX_ROWS) {
            rows = rows.subList(0, EXPORT_MAX_ROWS);
        }

        String fileName = "商品品牌_" + LocalDate.now() + ".xlsx";
        writeWorkbook(response, fileName, "商品品牌", rows);
    }

    /** 品牌导出列固定为 品牌名称/助记码/备注（对标数据表列） */
    private void writeWorkbook(HttpServletResponse response, String fileName, String sheetName,
                               List<ProductBrand> rows) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
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
            for (ProductBrand row : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(row.getBrandName() == null ? "" : row.getBrandName());
                excelRow.createCell(1).setCellValue(row.getMnemonicCode() == null ? "" : row.getMnemonicCode());
                excelRow.createCell(2).setCellValue(row.getRemark() == null ? "" : row.getRemark());
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}
