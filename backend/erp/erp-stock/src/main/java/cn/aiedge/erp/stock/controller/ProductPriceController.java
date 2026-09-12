package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.BatchModifyPriceDTO;
import cn.aiedge.erp.stock.dto.GradePriceVO;
import cn.aiedge.erp.stock.dto.ProductPriceVO;
import cn.aiedge.erp.stock.entity.CustomerGradeDiscount;
import cn.aiedge.erp.stock.entity.CustomerGradePrice;
import cn.aiedge.erp.stock.entity.CustomerProductPrice;
import cn.aiedge.erp.stock.service.ProductPriceService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 商品价格管理（资料 → 商品管理 → 商品价格管理）
 * <p>
 * 对标 ql361 四子标签：商品价格批量修改 / 客户级别折扣设置 / 级别指定价设置 / 客户指定价设置。
 * 商品价格单一口径：读写 {@code erp_product} / {@code erp_product_unit}，与《商品》《批量改价》一致。
 */
@Slf4j
@Tag(name = "商品价格管理")
@RestController
@RequestMapping("/api/erp/md/product-price")
@RequiredArgsConstructor
public class ProductPriceController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int EXPORT_MAX_ROWS = 10000;

    private final ProductPriceService productPriceService;

    // ══════════════════════ 子标签 1：商品价格批量修改 ══════════════════════

    @Operation(summary = "商品价格分页查询（商品 × 单位）")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/page")
    public Result<IPage<ProductPriceVO>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer shelfStatus,
            @RequestParam(required = false) String purchaseDateOp,
            @RequestParam(required = false) String purchaseDate,
            @RequestParam(required = false) String stockQtyOp,
            @RequestParam(required = false) String stockQty,
            @RequestParam(required = false) Boolean showHierarchy,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productPriceService.pagePrices(categoryId, keyword, brand, unitType, productId, shelfStatus,
                purchaseDateOp, purchaseDate, stockQtyOp, stockQty, showHierarchy,
                pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20));
    }

    @Operation(summary = "商品价格导出（真实 Excel 流）")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String unitType,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer shelfStatus,
            @RequestParam(required = false) String purchaseDateOp,
            @RequestParam(required = false) String purchaseDate,
            @RequestParam(required = false) String stockQtyOp,
            @RequestParam(required = false) String stockQty,
            @RequestParam(required = false) Boolean showHierarchy,
            HttpServletResponse response) throws IOException {
        List<ProductPriceVO> rows = productPriceService.listPrices(categoryId, keyword, brand, unitType, productId,
                shelfStatus, purchaseDateOp, purchaseDate, stockQtyOp, stockQty, showHierarchy);
        List<String[]> columnList = new ArrayList<>(List.of(
                new String[]{"imageUrl", "图片"}, new String[]{"shelfStatusText", "上架"},
                new String[]{"productCode", "商品货号"}, new String[]{"productName", "商品名称"},
                new String[]{"unitName", "单位"}, new String[]{"brand", "品牌"},
                new String[]{"conversionRelation", "换算关系"}, new String[]{"barcode", "条码"},
                new String[]{"recentPurchasePrice", "最近进价"}, new String[]{"presetPurchasePrice", "预设进价"},
                new String[]{"referenceCost", "参考成本"}, new String[]{"stockQty", "账面库存"},
                new String[]{"costAvgPrice", "成本均价"}, new String[]{"wholesalePrice", "批发价"},
                new String[]{"minDiscount", "最低折扣(%)"}, new String[]{"minSalePrice", "最低售价"},
                new String[]{"retailPrice", "零售价"}));
        List<String> priceGradeNames = productPriceService.listGradeNames();
        for (int i = 0; i < priceGradeNames.size(); i++) {
            columnList.add(new String[]{"gradePrice" + (i + 1), priceGradeNames.get(i)});
        }
        columnList.addAll(List.of(
                new String[]{"spec", "规格"}, new String[]{"lastPurchaseDate", "最近进货日期"},
                new String[]{"model", "型号"}, new String[]{"origin", "产地"}));
        String[][] columns = columnList.toArray(new String[0][]);
        writeExcel(response, "商品价格管理", columns, rows, r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("imageUrl", r.getImageUrl());
            map.put("shelfStatusText", r.getShelfStatusText());
            map.put("productCode", r.getProductCode());
            map.put("productName", r.getProductName());
            map.put("unitName", r.getUnitName());
            map.put("brand", r.getBrand());
            map.put("conversionRelation", r.getConversionRelation());
            map.put("barcode", r.getBarcode());
            map.put("recentPurchasePrice", r.getRecentPurchasePrice());
            map.put("presetPurchasePrice", r.getPresetPurchasePrice());
            map.put("referenceCost", r.getReferenceCost());
            map.put("stockQty", r.getStockQty());
            map.put("costAvgPrice", r.getCostAvgPrice());
            map.put("wholesalePrice", r.getWholesalePrice());
            map.put("minDiscount", r.getMinDiscount());
            map.put("minSalePrice", r.getMinSalePrice());
            map.put("retailPrice", r.getRetailPrice());
            map.put("gradePrice1", r.getGradePrice1());
            map.put("gradePrice2", r.getGradePrice2());
            map.put("gradePrice3", r.getGradePrice3());
            map.put("gradePrice4", r.getGradePrice4());
            map.put("gradePrice5", r.getGradePrice5());
            map.put("gradePrice6", r.getGradePrice6());
            map.put("gradePrice7", r.getGradePrice7());
            map.put("gradePrice8", r.getGradePrice8());
            map.put("spec", r.getSpec());
            map.put("lastPurchaseDate", r.getLastPurchaseDate() == null ? null : r.getLastPurchaseDate().toString());
            map.put("model", r.getModel());
            map.put("origin", r.getOrigin());
            return map;
        });
    }

    @Operation(summary = "批量修改商品价格")
    @SaCheckPermission("erp:product:price-batch")
    @PutMapping("/batch-modify")
    public Result<Integer> batchModify(@RequestBody BatchModifyPriceDTO dto) {
        return Result.ok(productPriceService.batchModifyPrices(dto));
    }

    @Operation(summary = "品牌下拉")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/brands")
    public Result<List<String>> brands() {
        return Result.ok(productPriceService.listBrands());
    }

    @Operation(summary = "客户级别下拉")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/customer-grades")
    public Result<List<String>> customerGrades() {
        return Result.ok(productPriceService.listCustomerGrades());
    }

    @Operation(summary = "价格等级名称列表（列标题动态渲染）")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/grades")
    public Result<List<String>> grades() {
        return Result.ok(productPriceService.listGradeNames());
    }

    @Operation(summary = "统一取价：客户/商品/单位 → 最终售价（含命中规则）")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/resolve")
    public Result<Map<String, Object>> resolve(@RequestParam Long productId,
                                               @RequestParam(required = false) Long customerId,
                                               @RequestParam(required = false) Long unitId) {
        return Result.ok(productPriceService.resolvePrice(customerId, productId, unitId));
    }

    // ══════════════════════ 子标签 2：客户级别折扣设置 ══════════════════════

    @Operation(summary = "客户级别折扣分页")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/grade-discount/page")
    public Result<IPage<CustomerGradeDiscount>> gradeDiscountPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productPriceService.pageGradeDiscounts(keyword,
                pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20));
    }

    @Operation(summary = "客户级别折扣导出")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/grade-discount/export")
    public void gradeDiscountExport(@RequestParam(required = false) String keyword,
                                    HttpServletResponse response) throws IOException {
        List<CustomerGradeDiscount> rows = productPriceService.listGradeDiscounts(keyword);
        String[][] columns = {
                {"gradeName", "客户级别"}, {"previewText", "级别默认价"},
                {"lastModifierName", "最后修改人"}, {"updateTimeText", "最后修改时间"},
        };
        writeExcel(response, "客户级别折扣设置", columns, rows, r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("gradeName", r.getGradeName());
            map.put("previewText", r.getPreviewText());
            map.put("lastModifierName", r.getLastModifierName());
            map.put("updateTimeText", r.getUpdateTime() == null ? null : r.getUpdateTime().format(DATE_TIME_FORMATTER));
            return map;
        });
    }

    @Operation(summary = "保存客户级别折扣（新增/修改）")
    @SaCheckPermission("erp:product:price-batch")
    @PostMapping("/grade-discount/save")
    public Result<Boolean> saveGradeDiscount(@RequestBody CustomerGradeDiscount entity) {
        productPriceService.saveGradeDiscount(entity);
        return Result.ok(true);
    }

    @Operation(summary = "删除客户级别折扣")
    @SaCheckPermission("erp:product:price-batch")
    @DeleteMapping("/grade-discount/{id}")
    public Result<Boolean> deleteGradeDiscount(@PathVariable Long id) {
        productPriceService.deleteGradeDiscount(id);
        return Result.ok(true);
    }

    // ══════════════════════ 子标签 3：级别指定价设置 ══════════════════════

    @Operation(summary = "级别指定价分页")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/level-price/page")
    public Result<IPage<GradePriceVO>> levelPricePage(
            @RequestParam(required = false) String gradeName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productPriceService.pageLevelPrices(gradeName, keyword, brand,
                pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20));
    }

    @Operation(summary = "级别指定价导出")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/level-price/export")
    public void levelPriceExport(@RequestParam(required = false) String gradeName,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String brand,
                                 HttpServletResponse response) throws IOException {
        List<GradePriceVO> rows = productPriceService.listLevelPrices(gradeName, keyword, brand);
        writeExcel(response, "级别指定价设置", ruleExportColumns(false), rows, this::toRuleExportRow);
    }

    @Operation(summary = "保存级别指定价（新增/修改）")
    @SaCheckPermission("erp:product:price-batch")
    @PostMapping("/level-price/save")
    public Result<Boolean> saveLevelPrice(@RequestBody CustomerGradePrice entity) {
        productPriceService.saveLevelPrice(entity);
        return Result.ok(true);
    }

    @Operation(summary = "删除级别指定价（支持批量）")
    @SaCheckPermission("erp:product:price-batch")
    @PostMapping("/level-price/batch-delete")
    public Result<Boolean> deleteLevelPrices(@RequestBody Map<String, List<Long>> body) {
        productPriceService.deleteLevelPrices(body.get("ids"));
        return Result.ok(true);
    }

    @Operation(summary = "级别指定价导入")
    @SaCheckPermission("erp:product:price-batch")
    @PostMapping("/level-price/import")
    public Result<Map<String, Object>> importLevelPrices(@RequestBody Map<String, List<Map<String, Object>>> body) {
        return Result.ok(productPriceService.importRules("level", body.get("rows")));
    }

    // ══════════════════════ 子标签 4：客户指定价设置 ══════════════════════

    @Operation(summary = "客户指定价分页")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/customer-price/page")
    public Result<IPage<GradePriceVO>> customerPricePage(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productPriceService.pageCustomerPrices(customerId, productId, keyword, brand,
                pageNum != null ? pageNum : 1, pageSize != null ? pageSize : 20));
    }

    @Operation(summary = "客户指定价导出")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/customer-price/export")
    public void customerPriceExport(@RequestParam(required = false) Long customerId,
                                    @RequestParam(required = false) Long productId,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String brand,
                                    HttpServletResponse response) throws IOException {
        List<GradePriceVO> rows = productPriceService.listCustomerPrices(customerId, productId, keyword, brand);
        writeExcel(response, "客户指定价设置", ruleExportColumns(true), rows, row -> {
            Map<String, Object> map = toRuleExportRow(row);
            map.put("customerName", row.getCustomerName());
            map.put("customerCode", row.getCustomerCode());
            return map;
        });
    }

    @Operation(summary = "保存客户指定价（新增/修改）")
    @SaCheckPermission("erp:product:price-batch")
    @PostMapping("/customer-price/save")
    public Result<Boolean> saveCustomerPrice(@RequestBody CustomerProductPrice entity) {
        productPriceService.saveCustomerPrice(entity);
        return Result.ok(true);
    }

    @Operation(summary = "删除客户指定价（支持批量）")
    @SaCheckPermission("erp:product:price-batch")
    @PostMapping("/customer-price/batch-delete")
    public Result<Boolean> deleteCustomerPrices(@RequestBody Map<String, List<Long>> body) {
        productPriceService.deleteCustomerPrices(body.get("ids"));
        return Result.ok(true);
    }

    @Operation(summary = "客户指定价导入")
    @SaCheckPermission("erp:product:price-batch")
    @PostMapping("/customer-price/import")
    public Result<Map<String, Object>> importCustomerPrices(@RequestBody Map<String, List<Map<String, Object>>> body) {
        return Result.ok(productPriceService.importRules("customer", body.get("rows")));
    }

    // ══════════════════════ Excel 模板 / 导入（对标「基本信息导入」三步向导） ══════════════════════

    @Operation(summary = "下载指定价导入模板")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/import-template")
    public void importTemplate(@RequestParam(defaultValue = "level") String type,
                               HttpServletResponse response) throws IOException {
        boolean isCustomer = "customer".equalsIgnoreCase(type);
        String subject = isCustomer ? "客户指定价" : "级别指定价";
        List<String> headers = new ArrayList<>();
        List<String> comments = new ArrayList<>();
        if (isCustomer) {
            headers.add("客户名称(必填)");
            comments.add("往来单位中的客户名称，需与客户档案完全一致");
        } else {
            headers.add("客户级别(必填)");
            comments.add("客户对应的级别名称，需与客户档案中的客户级别一致");
        }
        headers.add("商品名称(必填)");
        comments.add("商品档案中的商品名称，导入时按名称匹配商品");
        headers.add("货号");
        comments.add("选填，仅作备注");
        headers.add("单位");
        comments.add("选填，商品单位名称，如「箱」「瓶」");
        headers.add("基础价");
        comments.add("选填，取值如：零售价 / 批发价 / 价格等级名称；留空表示直接指定价");
        headers.add("计算符");
        comments.add("选填，+ - * / 之一，默认 +");
        headers.add("计算数(必填)");
        comments.add("必填，指定价时填金额；按基础价计算时填数值");

        String fileName = subject + "导入模板_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(subject);
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 22 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    @Operation(summary = "导入指定价（multipart Excel）")
    @SaCheckPermission("erp:product:price-batch")
    @PostMapping("/import-excel")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(defaultValue = "level") String type) throws IOException {
        boolean isCustomer = "customer".equalsIgnoreCase(type);
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headRow = sheet.getRow(sheet.getFirstRowNum());
            if (headRow == null) {
                throw new RuntimeException("模板缺少表头行");
            }
            Map<Integer, String> headerMap = new HashMap<>();
            DataFormatter formatter = new DataFormatter();
            for (Cell cell : headRow) {
                String header = formatter.formatCellValue(cell).trim();
                String field = headerField(header);
                if (field != null) {
                    headerMap.put(cell.getColumnIndex(), field);
                }
            }
            String requiredField = isCustomer ? "customerName" : "gradeName";
            if (!headerMap.containsValue(requiredField) || !headerMap.containsValue("productName")) {
                throw new RuntimeException("模板缺少必填列，请使用系统下载的模板");
            }
            for (int rowIdx = sheet.getFirstRowNum() + 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                Map<String, Object> values = new HashMap<>();
                headerMap.forEach((col, field) -> {
                    Cell cell = row.getCell(col);
                    values.put(field, cell == null ? null : formatter.formatCellValue(cell).trim());
                });
                Object productName = values.get("productName");
                if (productName == null || String.valueOf(productName).isEmpty()) {
                    continue;   // 整行为空跳过，不计入总数
                }
                rows.add(values);
            }
        }
        return Result.ok(productPriceService.importRules(type, rows));
    }

    /** 模板表头 → 内部字段名（与下载模板严格对应） */
    private String headerField(String header) {
        if (header == null || header.isEmpty()) {
            return null;
        }
        if (header.startsWith("客户名称")) return "customerName";
        if (header.startsWith("客户级别")) return "gradeName";
        if (header.startsWith("商品名称")) return "productName";
        if (header.startsWith("货号")) return "productCode";
        if (header.startsWith("单位")) return "unitName";
        if (header.startsWith("基础价")) return "basePriceType";
        if (header.startsWith("计算符")) return "calcOperator";
        if (header.startsWith("计算数")) return "calcValue";
        return null;
    }

    // ══════════════════════ 导出通用实现 ══════════════════════

    /**
     * 级别指定价 / 客户指定价导出列（对标列配置全量列）。
     * 价格等级列标题取自 erp_product_grade.grade_name，与页面列标题同源（等级改名后导出表头同步）。
     */
    private String[][] ruleExportColumns(boolean withCustomer) {
        List<String[]> cols = new ArrayList<>();
        if (withCustomer) {
            cols.add(new String[]{"customerName", "客户名称"});
            cols.add(new String[]{"customerCode", "客户编号"});
        } else {
            cols.add(new String[]{"gradeName", "客户级别"});
        }
        cols.addAll(List.of(
                new String[]{"targetName", "商品/分类名称"}, new String[]{"productCode", "货号"},
                new String[]{"unitName", "单位"}, new String[]{"priceRule", "价格规则"},
                new String[]{"barcode", "条码"}, new String[]{"spec", "规格"},
                new String[]{"model", "型号"}, new String[]{"brand", "品牌"},
                new String[]{"presetPurchasePrice", "预设进价"}, new String[]{"retailPrice", "零售价"},
                new String[]{"wholesalePrice", "批发价"}));
        List<String> priceGradeNames = productPriceService.listGradeNames();
        for (int i = 0; i < priceGradeNames.size(); i++) {
            cols.add(new String[]{"gradePrice" + (i + 1), priceGradeNames.get(i)});
        }
        cols.add(new String[]{"lastModifierName", "最后修改人"});
        cols.add(new String[]{"lastModifyTime", "最后修改时间"});
        return cols.toArray(new String[0][]);
    }

    private Map<String, Object> toRuleExportRow(GradePriceVO row) {
        Map<String, Object> map = new HashMap<>();
        map.put("gradeName", row.getGradeName());
        map.put("targetName", row.getTargetName());
        map.put("productCode", row.getProductCode());
        map.put("unitName", row.getUnitName());
        map.put("priceRule", row.getPriceRule());
        map.put("barcode", row.getBarcode());
        map.put("spec", row.getSpec());
        map.put("model", row.getModel());
        map.put("brand", row.getBrand());
        map.put("presetPurchasePrice", row.getPresetPurchasePrice());
        map.put("retailPrice", row.getRetailPrice());
        map.put("wholesalePrice", row.getWholesalePrice());
        map.put("gradePrice1", row.getGradePrice1());
        map.put("gradePrice2", row.getGradePrice2());
        map.put("gradePrice3", row.getGradePrice3());
        map.put("gradePrice4", row.getGradePrice4());
        map.put("gradePrice5", row.getGradePrice5());
        map.put("gradePrice6", row.getGradePrice6());
        map.put("gradePrice7", row.getGradePrice7());
        map.put("gradePrice8", row.getGradePrice8());
        map.put("lastModifierName", row.getLastModifierName());
        map.put("lastModifyTime", row.getLastModifyTime() == null ? null
                : row.getLastModifyTime().format(DATE_TIME_FORMATTER));
        return map;
    }

    private <T> void writeExcel(HttpServletResponse response, String sheetName, String[][] columns,
                                List<T> rows, Function<T, Map<String, Object>> rowMapper) throws IOException {
        if (rows == null) {
            rows = new ArrayList<>();
        }
        if (rows.size() > EXPORT_MAX_ROWS) {
            log.warn("导出结果超上限，已截断: total={}, max={}", rows.size(), EXPORT_MAX_ROWS);
            rows = rows.subList(0, EXPORT_MAX_ROWS);
        }
        String fileName = sheetName + "_" + LocalDate.now() + ".xlsx";
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
            for (int i = 0; i < columns.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(columns[i][1]);
                cell.setCellStyle(headStyle);
            }
            int rowIdx = 1;
            for (T row : rows) {
                Map<String, Object> values = rowMapper.apply(row);
                Row excelRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < columns.length; i++) {
                    writeCell(excelRow.createCell(i), values.get(columns[i][0]));
                }
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(response.getOutputStream());
        }
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
