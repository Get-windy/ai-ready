package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.WarehouseQuery;
import cn.aiedge.erp.stock.entity.Warehouse;
import cn.aiedge.erp.stock.service.WarehouseService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
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
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 仓库Controller（资料 → 仓库管理 → 仓库规划）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "仓库管理", description = "仓库规划接口")
@RestController
@RequestMapping("/api/erp/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    /** 导出上限（防止一次性拉全表） */
    private static final int EXPORT_MAX_ROWS = 10000;
    /** 导出列：字段名 / 表头 */
    private static final String[][] EXPORT_COLUMNS = {
            {"warehouseCode", "仓库编号"},
            {"warehouseName", "仓库名称"},
            {"contactPerson", "联系人"},
            {"contactPhone", "联系电话"},
            {"address", "地址"}
    };

    private final WarehouseService warehouseService;

    @Operation(summary = "查询仓库列表")
    @SaCheckPermission("wms:list")
    @GetMapping("/list")
    public Result<List<Warehouse>> list() {
        List<Warehouse> list = warehouseService.getWarehouseList();
        return Result.ok(list);
    }

    @Operation(summary = "仓库规划分页查询")
    @SaCheckPermission("wms:list")
    @GetMapping("/page")
    public Result<Page<Warehouse>> page(WarehouseQuery query) {
        return Result.ok(warehouseService.pageWarehouse(query));
    }

    @Operation(summary = "生成下一个仓库编号")
    @SaCheckPermission("wms:view")
    @GetMapping("/next-code")
    public Result<String> nextCode() {
        return Result.ok(warehouseService.nextCode());
    }

    @Operation(summary = "仓库详情")
    @SaCheckPermission("wms:detail")
    @GetMapping("/{id}")
    public Result<Warehouse> getById(@PathVariable Long id) {
        Warehouse warehouse = warehouseService.getWarehouseDetail(id);
        return warehouse == null ? Result.fail("仓库不存在") : Result.ok(warehouse);
    }

    @Operation(summary = "新增仓库")
    @SaCheckPermission("wms:create")
    @PostMapping("/save")
    public Result<Warehouse> save(@RequestBody Warehouse warehouse) {
        return Result.ok(warehouseService.createWarehouse(warehouse));
    }

    @Operation(summary = "修改仓库")
    @SaCheckPermission("wms:create")
    @PostMapping("/update")
    public Result<Boolean> update(@RequestBody Warehouse warehouse) {
        return Result.ok(warehouseService.updateWarehouse(warehouse));
    }

    @Operation(summary = "启用/停用仓库")
    @SaCheckPermission("wms:create")
    @PostMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id,
                                        @RequestParam(required = false) Integer status) {
        return Result.ok(warehouseService.updateStatus(id, status));
    }

    @Operation(summary = "删除仓库")
    @SaCheckPermission("wms:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(warehouseService.removeWarehouse(id));
    }

    @Operation(summary = "导出仓库列表（xlsx）")
    @SaCheckPermission("wms:export")
    @GetMapping("/export")
    public void export(WarehouseQuery query, HttpServletResponse response) throws IOException {
        query.setPageSize(EXPORT_MAX_ROWS);
        List<Warehouse> list = warehouseService.listForExport(query);

        String fileName = "仓库规划_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("仓库规划");
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
            for (Warehouse w : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullToEmpty(w.getWarehouseCode()));
                row.createCell(1).setCellValue(nullToEmpty(w.getWarehouseName()));
                row.createCell(2).setCellValue(nullToEmpty(w.getContactPerson()));
                row.createCell(3).setCellValue(nullToEmpty(w.getContactPhone()));
                row.createCell(4).setCellValue(nullToEmpty(w.getAddress()));
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sheet.setColumnWidth(i, 22 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
