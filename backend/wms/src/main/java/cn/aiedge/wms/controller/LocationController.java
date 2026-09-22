package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.entity.WmsLocation;
import cn.aiedge.wms.warehouse.dto.LocationGenerateDTO;
import cn.aiedge.wms.warehouse.dto.LocationQuery;
import cn.aiedge.wms.warehouse.service.WarehouseService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Validated
@Tag(name = "货位管理")
@RestController
@RequestMapping("/api/wms/location")
@RequiredArgsConstructor
public class LocationController {

    private final WarehouseService warehouseService;

    @Operation(summary = "新增货位")
    @SaCheckPermission("wms:location:create")
    @PostMapping("/save")
    public Result<WmsLocation> save(@Valid @RequestBody WmsLocation location) {
        warehouseService.saveLocation(location);
        log.info("新增货位: id={}, code={}", location.getId(), location.getLocationCode());
        return Result.ok(location);
    }

    @Operation(summary = "更新货位")
    @SaCheckPermission("wms:location:create")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsLocation location) {
        boolean updated = warehouseService.updateLocation(location);
        if (updated) {
            log.info("更新货位: id={}", location.getId());
        }
        return Result.ok(updated);
    }

    @Operation(summary = "根据ID查询货位")
    @SaCheckPermission("wms:location:detail")
    @GetMapping("/{id}")
    public Result<WmsLocation> getById(@PathVariable @NotNull(message = "货位ID不能为空") Long id) {
        WmsLocation location = warehouseService.getLocationById(id);
        if (location == null) {
            return Result.fail("货位不存在");
        }
        return Result.ok(location);
    }

    @Operation(summary = "分页查询货位")
    @SaCheckPermission("wms:location:list")
    @GetMapping("/page")
    public Result<Page<WmsLocation>> page(@Valid Page<WmsLocation> page, WmsLocation query) {
        return Result.ok(warehouseService.pageLocation(page, query));
    }

    @Operation(summary = "删除货位")
    @SaCheckPermission("wms:location:delete")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "货位ID不能为空") Long id) {
        warehouseService.removeLocation(id);
        log.info("删除货位: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "根据仓库查询货位列表")
    @SaCheckPermission("wms:location:list")
    @GetMapping("/list-by-warehouse/{warehouseId}")
    public Result<List<WmsLocation>> listByWarehouse(
            @PathVariable @NotNull(message = "仓库ID不能为空") Long warehouseId) {
        return Result.ok(warehouseService.listByWarehouseId(warehouseId));
    }

    @Operation(summary = "推荐上架货位")
    @SaCheckPermission("wms:location:view")
    @GetMapping("/recommend")
    public Result<List<WmsLocation>> recommend(
            @RequestParam @NotNull Long warehouseId,
            @RequestParam @NotNull Long productId,
            @RequestParam @Positive BigDecimal quantity) {
        return Result.ok(warehouseService.recommendLocations(warehouseId, productId, quantity));
    }

    // ══════════════════════════════════════════════════════════════════════
    // 仓库规划 → 2.货位（对标 ql361）
    // ══════════════════════════════════════════════════════════════════════

    @Operation(summary = "货位列表分页（仓库/货位编号/显示停用）")
    @SaCheckPermission("wms:location:view")
    @GetMapping("/plan-page")
    public Result<Page<WmsLocation>> planPage(LocationQuery query) {
        return Result.ok(warehouseService.pageLocationPlan(query));
    }

    @Operation(summary = "批量生成货位")
    @SaCheckPermission("wms:location:generate")
    @PostMapping("/generate")
    public Result<Integer> generate(@RequestBody LocationGenerateDTO dto) {
        int created = warehouseService.generateLocations(dto);
        log.info("批量生成货位: warehouseId={}, count={}", dto.getWarehouseId(), created);
        return Result.ok(created);
    }

    @Operation(summary = "启用/停用货位")
    @SaCheckPermission("wms:location:create")
    @PostMapping("/{id}/enabled")
    public Result<Boolean> updateEnabled(@PathVariable Long id,
                                         @RequestParam(required = false) Integer isEnabled) {
        return Result.ok(warehouseService.updateLocationEnabled(id, isEnabled));
    }

    @Operation(summary = "批量删除货位")
    @SaCheckPermission("wms:location:delete")
    @PostMapping("/batch-delete")
    public Result<Integer> batchDelete(@RequestBody List<Long> ids) {
        return Result.ok(warehouseService.batchRemoveLocations(ids));
    }

    @Operation(summary = "导出货位列表（xlsx）")
    @SaCheckPermission("wms:location:export")
    @GetMapping("/export")
    public void export(LocationQuery query, HttpServletResponse response) throws IOException {
        List<WmsLocation> list = warehouseService.listLocationsForExport(query);

        String fileName = "货位_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {"所属仓库", "货位编号", "备注"};
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("货位");
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
            for (WmsLocation location : list) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(location.getWarehouseName() == null ? "" : location.getWarehouseName());
                row.createCell(1).setCellValue(location.getLocationCode() == null ? "" : location.getLocationCode());
                row.createCell(2).setCellValue(location.getRemark() == null ? "" : location.getRemark());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 22 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }
}
