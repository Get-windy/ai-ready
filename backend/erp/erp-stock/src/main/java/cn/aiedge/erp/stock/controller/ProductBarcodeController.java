package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.ProductBarcodeVO;
import cn.aiedge.erp.stock.entity.ProductBarcode;
import cn.aiedge.erp.stock.service.ProductBarcodeService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "产品条形码管理")
@RestController
@RequestMapping("/api/erp/product/barcodes")
@RequiredArgsConstructor
public class ProductBarcodeController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int EXPORT_MAX_ROWS = 10000;

    /** 导出列（与页面列配置 12 列一致）：字段名 + 表头 */
    private static final String[][] EXPORT_COLUMNS = {
            {"imageUrl", "图片"},
            {"productName", "商品名称"},
            {"productCode", "货号"},
            {"unitName", "单位"},
            {"conversionRelation", "换算关系"},
            {"shelfStatusText", "上架状态"},
            {"barcode", "条码"},
            {"spec", "规格"},
            {"model", "型号"},
            {"origin", "产地"},
            {"createTimeText", "新增时间"},
            {"lastPurchaseDateText", "最近采购日期"},
    };

    private final ProductBarcodeService productBarcodeService;

    @Operation(summary = "查询产品条形码列表")
    @GetMapping("/{productId}")
    public Result<List<ProductBarcode>> getByProduct(@PathVariable Long productId) {
        return Result.ok(productBarcodeService.getByProductId(productId));
    }

    @Operation(summary = "商品条码分页查询（商品 × 单位，含隐藏列字段）")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/page")
    public Result<IPage<ProductBarcodeVO>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String barcodeFilter,
            @RequestParam(required = false) Integer shelfStatus,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createTimeOp,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String purchaseDateOp,
            @RequestParam(required = false) String purchaseDateStart,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productBarcodeService.pageBarcodes(categoryId, keyword, barcodeFilter,
                shelfStatus, status, createTimeOp, createTimeStart, purchaseDateOp, purchaseDateStart,
                sortField, sortOrder,
                pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20));
    }

    @Operation(summary = "导出商品条码（真实 Excel 流）")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String barcodeFilter,
            @RequestParam(required = false) Integer shelfStatus,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createTimeOp,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String purchaseDateOp,
            @RequestParam(required = false) String purchaseDateStart,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            HttpServletResponse response) throws IOException {
        List<ProductBarcodeVO> rows = productBarcodeService.listBarcodes(categoryId, keyword, barcodeFilter,
                shelfStatus, status, createTimeOp, createTimeStart, purchaseDateOp, purchaseDateStart,
                sortField, sortOrder);
        if (rows.size() > EXPORT_MAX_ROWS) {
            log.warn("商品条码导出结果超上限，已截断: total={}, max={}", rows.size(), EXPORT_MAX_ROWS);
            rows = rows.subList(0, EXPORT_MAX_ROWS);
        }

        String fileName = "商品条码_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("商品条码");
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
            for (ProductBarcodeVO row : rows) {
                Map<String, Object> values = toExportRow(row);
                Row excelRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                    writeCell(excelRow.createCell(i), values.get(EXPORT_COLUMNS[i][0]));
                }
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    @Operation(summary = "行级修改：维护商品单位条码")
    @SaCheckPermission("erp:product:update")
    @PutMapping("/unit/{unitId}")
    public Result<Boolean> updateUnitBarcode(@PathVariable Long unitId,
                                             @RequestBody Map<String, String> body) {
        productBarcodeService.updateUnitBarcode(unitId, body.get("barcode"), body.get("barcodeType"));
        return Result.ok(true);
    }

    @Operation(summary = "新增条形码")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductBarcode barcode) {
        return Result.ok(productBarcodeService.save(barcode));
    }

    @Operation(summary = "更新条形码")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody ProductBarcode barcode) {
        barcode.setId(id);
        return Result.ok(productBarcodeService.updateById(barcode));
    }

    @Operation(summary = "删除条形码")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productBarcodeService.removeById(id));
    }

    private Map<String, Object> toExportRow(ProductBarcodeVO vo) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("imageUrl", vo.getImageUrl());
        map.put("productName", vo.getProductName());
        map.put("productCode", vo.getProductCode());
        map.put("unitName", vo.getUnitName());
        map.put("conversionRelation", vo.getConversionRelation());
        map.put("shelfStatusText", vo.getShelfStatusText());
        map.put("barcode", vo.getBarcode());
        map.put("spec", vo.getSpec());
        map.put("model", vo.getModel());
        map.put("origin", vo.getOrigin());
        LocalDateTime createTime = vo.getCreateTime();
        map.put("createTimeText", createTime == null ? null : createTime.format(DATE_TIME_FORMATTER));
        map.put("lastPurchaseDateText", vo.getLastPurchaseDate() == null ? null : vo.getLastPurchaseDate().toString());
        return map;
    }

    private void writeCell(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof BigDecimal decimal) {
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }
}
