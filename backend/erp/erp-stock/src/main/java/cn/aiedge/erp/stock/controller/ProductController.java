package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.BatchPriceUpdateDTO;
import cn.aiedge.erp.stock.dto.BatchStatusUpdateDTO;
import cn.aiedge.erp.stock.dto.ProductFormDTO;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.entity.ProductRecommend;
import cn.aiedge.erp.stock.entity.ProductUnit;
import cn.aiedge.erp.stock.service.ProductBarcodeService;
import cn.aiedge.erp.stock.service.ProductRecommendService;
import cn.aiedge.erp.stock.service.ProductService;
import cn.aiedge.erp.stock.service.ProductUnitService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 产品Controller - 产品列表与详情管理
 */
@Slf4j
@Tag(name = "产品管理")
@RestController
@RequestMapping("/api/erp/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductUnitService productUnitService;
    private final ProductBarcodeService productBarcodeService;
    private final ProductRecommendService productRecommendService;
    private final cn.aiedge.erp.stock.service.CloudProductService cloudProductService;
    /** Spring 容器中的 ObjectMapper（已注册 JavaTimeModule，支持 LocalDateTime 等字段） */
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Operation(summary = "查询产品列表")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/list")
    public Result<List<Product>> list() {
        return Result.ok(productService.getProductList());
    }

    @Operation(summary = "分页查询产品")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/page")
    public Result<IPage<Product>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String industryCategory,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) Integer useCoupon,
            @RequestParam(required = false) Integer isStandardProduct,
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) Integer mallShelfStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productService.getProductPage(categoryId, keyword, status, brand, industryCategory,
                createTimeStart, createTimeEnd, useCoupon, isStandardProduct, productType, mallShelfStatus,
                pageNum, pageSize));
    }

    @Operation(summary = "获取产品详情(含等级价格)")
    @SaCheckPermission("erp:product:view")
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.ok(productService.getProductDetail(id));
    }

    @Operation(summary = "获取产品表单数据(含单位+推荐)")
    @SaCheckPermission("erp:product:view")
    @GetMapping("/{id}/form")
    public Result<ProductFormDTO> getFormById(@PathVariable Long id) {
        Product product = productService.getProductDetail(id);
        if (product == null) {
            return Result.fail("商品不存在或已删除");
        }
        ProductFormDTO dto = new ProductFormDTO();
        dto.setProduct(product);
        dto.setUnits(productUnitService.getByProductId(id));
        dto.setRecommends(productRecommendService.getByProductId(id));
        return Result.ok(dto);
    }

    @Operation(summary = "按编码查询")
    @SaCheckPermission("erp:product:view")
    @GetMapping("/by-code/{productCode}")
    public Result<Product> getByCode(@PathVariable String productCode) {
        Product product = productService.lambdaQuery()
                .eq(Product::getProductCode, productCode)
                .eq(Product::getDeleted, 0)
                .one();
        return Result.ok(product);
    }

    @Operation(summary = "新增产品")
    @SaCheckPermission("erp:product:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody Product product) {
        return Result.ok(productService.createProduct(product));
    }

    @Operation(summary = "编辑产品")
    @SaCheckPermission("erp:product:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        return Result.ok(productService.updateProduct(product));
    }

    @Operation(summary = "更新产品状态")
    @SaCheckPermission("erp:product:status")
    @PutMapping("/{id}/status")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return Result.ok(productService.updateProductStatus(id, status));
    }

    @Operation(summary = "删除产品")
    @SaCheckPermission("erp:product:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productService.removeById(id));
    }

    @Operation(summary = "批量更新产品价格")
    @SaCheckPermission("erp:product:price-batch")
    @PutMapping("/batch-prices")
    public Result<Boolean> batchUpdatePrices(@RequestBody BatchPriceUpdateDTO dto) {
        return Result.ok(productService.batchUpdatePrices(dto.getItems()));
    }

    @Operation(summary = "导出产品列表(真实 xlsx)")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String industryCategory,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) Integer useCoupon,
            @RequestParam(required = false) Integer isStandardProduct,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        List<Product> rows = productService.exportList(categoryId, keyword, status,
                brand, industryCategory, createTimeStart, createTimeEnd,
                useCoupon, isStandardProduct);

        String fileName = "商品_" + java.time.LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8));

        String[] headers = {"图片地址", "商品名称", "商品货号", "所属行业类别", "条码", "规格", "型号", "产地",
                "品牌", "单位", "可用库存", "换算关系", "零售价", "批发价", "预设进价", "备注"};
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("商品");
            org.apache.poi.ss.usermodel.CellStyle headStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);
            org.apache.poi.ss.usermodel.Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }
            int rowIdx = 1;
            for (Product p : rows) {
                org.apache.poi.ss.usermodel.Row r = sheet.createRow(rowIdx++);
                int c = 0;
                r.createCell(c++).setCellValue(nz(p.getImageUrl()));
                r.createCell(c++).setCellValue(nz(p.getProductName()));
                r.createCell(c++).setCellValue(nz(p.getProductCodeAlias()));
                r.createCell(c++).setCellValue(nz(p.getIndustryCategory()));
                r.createCell(c++).setCellValue(nz(p.getBarcode()));
                r.createCell(c++).setCellValue(nz(p.getSpec()));
                r.createCell(c++).setCellValue(nz(p.getModel()));
                r.createCell(c++).setCellValue(nz(p.getOrigin()));
                r.createCell(c++).setCellValue(nz(p.getBrand()));
                r.createCell(c++).setCellValue(nz(p.getUnit()));
                r.createCell(c++).setCellValue(p.getAvailableStock() == null ? 0D : p.getAvailableStock().doubleValue());
                r.createCell(c++).setCellValue(nz(p.getConversionRelation()));
                r.createCell(c++).setCellValue(p.getRetailPrice() == null ? 0D : p.getRetailPrice().doubleValue());
                r.createCell(c++).setCellValue(p.getWholesalePrice() == null ? 0D : p.getWholesalePrice().doubleValue());
                r.createCell(c++).setCellValue(p.getPurchasePrice() == null ? 0D : p.getPurchasePrice().doubleValue());
                r.createCell(c++).setCellValue(nz(p.getRemark()));
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    @Operation(summary = "批量更新商品状态")
    @SaCheckPermission("erp:product:status")
    @PutMapping("/batch-status")
    public Result<Boolean> batchUpdateStatus(@RequestBody BatchStatusUpdateDTO dto) {
        return Result.ok(productService.batchUpdateStatus(dto.getIds(), dto.getStatus()));
    }

    @Operation(summary = "批量删除商品")
    @SaCheckPermission("erp:product:delete")
    @PutMapping("/batch-delete")
    public Result<Boolean> batchDelete(@RequestBody Map<String, List<Long>> body) {
        return Result.ok(productService.batchDelete(body.get("ids")));
    }

    @Operation(summary = "产品审批")
    @SaCheckPermission("erp:product:approval")
    @PutMapping("/{id}/approval")
    public Result<Boolean> approval(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String action = body.get("action");
        if (action == null) return Result.fail("审批操作不能为空");
        Product product = new Product();
        product.setId(id);
        product.setApprovalStatus(action);
        return Result.ok(productService.updateById(product));
    }

    @Operation(summary = "获取行业类别选项(去重)")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/industry-categories")
    public Result<List<String>> getIndustryCategories() {
        return Result.ok(productService.getDistinctIndustryCategories());
    }

    @Operation(summary = "获取品牌选项(去重)")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/brands")
    public Result<List<String>> getBrands() {
        return Result.ok(productService.getDistinctBrands());
    }

    @Operation(summary = "批量搬移分类")
    @SaCheckPermission("erp:product:update")
    @PutMapping("/batch-move")
    public Result<Integer> batchMove(@RequestBody Map<String, Object> body) {
        List<Long> ids = toLongList(body.get("ids"));
        Object categoryIdRaw = body.get("categoryId");
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请先选择商品");
        }
        if (categoryIdRaw == null) {
            return Result.fail("请选择目标分类");
        }
        Long categoryId = Long.valueOf(String.valueOf(categoryIdRaw));
        return Result.ok(productService.batchMoveCategory(ids, categoryId));
    }

    @Operation(summary = "批量修改商品字段(品牌/行业类别/分类/是否标品/使用优惠券)")
    @SaCheckPermission("erp:product:update")
    @PutMapping("/batch-update-fields")
    public Result<Integer> batchUpdateFields(@RequestBody Map<String, Object> body) {
        List<Long> ids = toLongList(body.get("ids"));
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请先选择商品");
        }
        Map<String, Object> fields = new java.util.HashMap<>();
        for (String key : new String[]{"brand", "industryCategory", "isStandardProduct", "useCoupon"}) {
            if (body.containsKey(key) && body.get(key) != null && !"".equals(body.get(key))) {
                fields.put(key, body.get(key));
            }
        }
        if (body.get("categoryId") != null && !"".equals(body.get("categoryId"))) {
            fields.put("categoryId", Long.valueOf(String.valueOf(body.get("categoryId"))));
        }
        if (fields.isEmpty()) {
            return Result.fail("请至少选择一个要修改的字段");
        }
        return Result.ok(productService.batchUpdateFields(ids, fields));
    }

    @Operation(summary = "批量上架/下架(商城)")
    @SaCheckPermission("erp:product:update")
    @PutMapping("/batch-shelf")
    public Result<Integer> batchShelf(@RequestBody Map<String, Object> body) {
        List<Long> ids = toLongList(body.get("ids"));
        Object shelfStatusRaw = body.get("mallShelfStatus");
        if (ids == null || ids.isEmpty()) {
            return Result.fail("请先选择商品");
        }
        if (shelfStatusRaw == null) {
            return Result.fail("请指定上架状态");
        }
        Integer shelfStatus = Integer.valueOf(String.valueOf(shelfStatusRaw));
        return Result.ok(productService.batchUpdateShelfStatus(ids, shelfStatus));
    }

    @Operation(summary = "设置商城默认排序方式")
    @SaCheckPermission("erp:product:update")
    @PutMapping("/set-mall-sort")
    public Result<Integer> setMallSort(@RequestBody Map<String, Object> body) {
        String sortType = body.get("sortType") == null ? "DEFAULT" : String.valueOf(body.get("sortType"));
        return Result.ok(productService.setMallSortType(sortType));
    }

    @Operation(summary = "导入商品(Excel)")
    @SaCheckPermission("erp:product:create")
    @PostMapping("/import")
    public Result<Map<String, Object>> importProducts(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return Result.ok(productService.importFromExcel(file));
    }

    @Operation(summary = "云商品库分页(云导入候选)")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/cloud-catalog/page")
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<cn.aiedge.erp.stock.entity.CloudProduct>> cloudCatalogPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String industryCategory,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(cloudProductService.getCloudPage(keyword, industryCategory, pageNum, pageSize));
    }

    @Operation(summary = "云导入(把云商品库条目导入为本地商品)")
    @SaCheckPermission("erp:product:create")
    @PostMapping("/cloud-import")
    public Result<Map<String, Object>> cloudImport(@RequestBody Map<String, Object> body) {
        List<Long> cloudIds = toLongList(body.get("cloudIds"));
        Object categoryIdRaw = body.get("categoryId");
        Long categoryId = categoryIdRaw == null ? null : Long.valueOf(String.valueOf(categoryIdRaw));
        return Result.ok(cloudProductService.cloudImport(cloudIds, categoryId));
    }

    @SuppressWarnings("unchecked")
    private static List<Long> toLongList(Object raw) {
        if (raw == null) {
            return null;
        }
        List<Long> list = new java.util.ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object o : (List<Object>) raw) {
                if (o == null) {
                    continue;
                }
                if (o instanceof Number) {
                    list.add(((Number) o).longValue());
                } else {
                    String s = String.valueOf(o).trim();
                    if (!s.isEmpty()) {
                        list.add(Long.valueOf(s));
                    }
                }
            }
        }
        return list;
    }

    @Operation(summary = "批量创建商品(含单位+推荐)")
    @SaCheckPermission("erp:product:create")
    @PostMapping("/batch-create")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> batchCreate(@RequestBody Map<String, Object> payload) {
        try {
            // 1. 保存产品主表
            Product product = new Product();
            @SuppressWarnings("unchecked")
            Map<String, Object> productMap = (Map<String, Object>) payload.get("product");
            if (productMap != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = objectMapper;
                product = mapper.convertValue(productMap, Product.class);
            }
            boolean saved = productService.createProduct(product);
            if (!saved) return Result.fail("产品保存失败");

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("productId", product.getId());

            // 2. 保存产品单位：先解析校验（失败不落库），再落库，最后同步条码表
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsData = (List<Map<String, Object>>) payload.get("units");
            if (unitsData != null && !unitsData.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = objectMapper;
                List<ProductUnit> savedUnits = new java.util.ArrayList<>();
                for (Map<String, Object> u : unitsData) {
                    ProductUnit unit = mapper.convertValue(u, ProductUnit.class);
                    unit.setProductId(product.getId());
                    savedUnits.add(unit);
                }
                productBarcodeService.validateUnitBarcodes(product.getId(), savedUnits);
                for (ProductUnit unit : savedUnits) {
                    productUnitService.save(unit);
                }
                productBarcodeService.syncUnitBarcodes(product.getId(), savedUnits);
            }

            // 3. 保存推荐商品
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendsData = (List<Map<String, Object>>) payload.get("recommends");
            if (recommendsData != null && !recommendsData.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = objectMapper;
                List<ProductRecommend> recommends = new java.util.ArrayList<>();
                for (Map<String, Object> r : recommendsData) {
                    ProductRecommend rec = mapper.convertValue(r, ProductRecommend.class);
                    rec.setProductId(product.getId());
                    recommends.add(rec);
                }
                productRecommendService.batchSave(product.getId(), recommends);
            }

            return Result.ok(result);
        } catch (Exception e) {
            log.error("[批量创建商品] 失败", e);
            // 异常被转换为 Result.fail 返回，需显式标记回滚，避免半成品数据（商品已建但单位/推荐失败）
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return Result.fail("创建失败: " + e.getMessage());
        }
    }

    @Operation(summary = "批量更新商品(含单位+推荐)")
    @SaCheckPermission("erp:product:update")
    @PutMapping("/batch-update/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> batchUpdate(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            // 1. 更新产品主表
            @SuppressWarnings("unchecked")
            Map<String, Object> productMap = (Map<String, Object>) payload.get("product");
            if (productMap != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = objectMapper;
                Product product = mapper.convertValue(productMap, Product.class);
                product.setId(id);
                productService.updateProduct(product);
            }

            // 2. 更新产品单位：先解析校验（失败不落库、不删旧行），再「先删后插」，最后同步条码表
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> unitsData = (List<Map<String, Object>>) payload.get("units");
            if (unitsData != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = objectMapper;
                List<ProductUnit> savedUnits = new java.util.ArrayList<>();
                for (Map<String, Object> u : unitsData) {
                    ProductUnit unit = mapper.convertValue(u, ProductUnit.class);
                    unit.setProductId(id);
                    unit.setId(null); // 强制新建
                    savedUnits.add(unit);
                }
                productBarcodeService.validateUnitBarcodes(id, savedUnits);
                productUnitService.lambdaUpdate()
                        .eq(ProductUnit::getProductId, id)
                        .remove();
                for (ProductUnit unit : savedUnits) {
                    productUnitService.save(unit);
                }
                productBarcodeService.syncUnitBarcodes(id, savedUnits);
            }

            // 3. 更新推荐商品
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendsData = (List<Map<String, Object>>) payload.get("recommends");
            if (recommendsData != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = objectMapper;
                List<ProductRecommend> recommends = new java.util.ArrayList<>();
                for (Map<String, Object> r : recommendsData) {
                    ProductRecommend rec = mapper.convertValue(r, ProductRecommend.class);
                    rec.setProductId(id);
                    recommends.add(rec);
                }
                productRecommendService.batchSave(id, recommends);
            }

            return Result.ok(true);
        } catch (Exception e) {
            log.error("[批量更新商品] 失败", e);
            // 单位表是「先删后插」：不标记回滚会在校验/插入失败时丢光原有单位行（P0）
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return Result.fail("更新失败: " + e.getMessage());
        }
    }
}
