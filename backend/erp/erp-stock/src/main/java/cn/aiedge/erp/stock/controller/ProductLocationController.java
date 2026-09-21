package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.ProductLocationQuery;
import cn.aiedge.erp.stock.dto.ProductLocationSetDTO;
import cn.aiedge.erp.stock.dto.ProductLocationVO;
import cn.aiedge.erp.stock.service.ProductLocationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 商品货位设置（资料 → 仓库管理 → 商品货位设置，菜单 70520 / md:location）
 * <p>
 * 对标 ql361 GoodsGPositionList：按商品维护其在指定仓库下的推荐货位。
 * 货位主数据复用 wms_location（全局唯一口径），本模块只维护绑定关系。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Tag(name = "商品货位设置")
@RestController
@RequestMapping("/api/erp/product-location")
@RequiredArgsConstructor
public class ProductLocationController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int EXPORT_MAX_ROWS = 10000;

    /** 导出列（与页面列配置 24 列的数据列一致）：表头 + 取值函数 */
    private static final Object[][] EXPORT_COLUMNS = {
            {"图片", (Function<ProductLocationVO, Object>) ProductLocationVO::getImageUrl},
            {"商品名称", (Function<ProductLocationVO, Object>) ProductLocationVO::getProductName},
            {"货号", (Function<ProductLocationVO, Object>) ProductLocationVO::getProductCode},
            {"上架", (Function<ProductLocationVO, Object>) vo -> Integer.valueOf(1).equals(vo.getShelfStatus()) ? "√" : "×"},
            {"单位", (Function<ProductLocationVO, Object>) ProductLocationVO::getUnit},
            {"条码", (Function<ProductLocationVO, Object>) ProductLocationVO::getBarcode},
            {"规格", (Function<ProductLocationVO, Object>) ProductLocationVO::getSpec},
            {"型号", (Function<ProductLocationVO, Object>) ProductLocationVO::getModel},
            {"产地", (Function<ProductLocationVO, Object>) ProductLocationVO::getOrigin},
            {"品牌", (Function<ProductLocationVO, Object>) ProductLocationVO::getBrand},
            {"仓库", (Function<ProductLocationVO, Object>) ProductLocationVO::getWarehouseName},
            {"推荐货位", (Function<ProductLocationVO, Object>) ProductLocationVO::getLocationCode},
            {"零售价", (Function<ProductLocationVO, Object>) ProductLocationVO::getRetailPrice},
            {"批发价", (Function<ProductLocationVO, Object>) ProductLocationVO::getWholesalePrice},
            {"价格等级1", (Function<ProductLocationVO, Object>) ProductLocationVO::getGradePrice1},
            {"价格等级2", (Function<ProductLocationVO, Object>) ProductLocationVO::getGradePrice2},
            {"价格等级3", (Function<ProductLocationVO, Object>) ProductLocationVO::getGradePrice3},
            {"价格等级4", (Function<ProductLocationVO, Object>) ProductLocationVO::getGradePrice4},
            {"价格等级5", (Function<ProductLocationVO, Object>) ProductLocationVO::getGradePrice5},
            {"价格等级6", (Function<ProductLocationVO, Object>) ProductLocationVO::getGradePrice6},
            {"价格等级7", (Function<ProductLocationVO, Object>) ProductLocationVO::getGradePrice7},
            {"价格等级8", (Function<ProductLocationVO, Object>) ProductLocationVO::getGradePrice8},
            {"备注", (Function<ProductLocationVO, Object>) ProductLocationVO::getRemark},
            {"修改时间", (Function<ProductLocationVO, Object>) vo ->
                    vo.getModifyTime() == null ? null : vo.getModifyTime().format(DATE_TIME_FORMATTER)},
    };

    private final ProductLocationService productLocationService;

    @Operation(summary = "商品货位设置分页查询（商品 × 仓库 → 推荐货位）")
    @SaCheckPermission("product:location:list")
    @GetMapping("/page")
    public Result<IPage<ProductLocationVO>> page(ProductLocationQuery query) {
        return Result.ok(productLocationService.page(query));
    }

    @Operation(summary = "设置商品推荐货位（支持批量，同一仓库）")
    @SaCheckPermission("product:location:create")
    @PostMapping("/set")
    public Result<Integer> set(@RequestBody ProductLocationSetDTO dto) {
        return Result.ok(productLocationService.setLocation(dto));
    }

    @Operation(summary = "批量移除商品推荐货位（同一仓库）")
    @SaCheckPermission("product:location:create")
    @PostMapping("/batch-remove")
    public Result<Integer> batchRemove(@RequestBody ProductLocationSetDTO dto) {
        return Result.ok(productLocationService.removeLocation(dto));
    }

    @Operation(summary = "导出商品货位设置（真实 Excel 流）")
    @SaCheckPermission("product:location:export")
    @GetMapping("/export")
    public void export(ProductLocationQuery query, HttpServletResponse response) throws IOException {
        List<ProductLocationVO> rows = productLocationService.list(query);
        if (rows.size() > EXPORT_MAX_ROWS) {
            log.warn("商品货位设置导出结果超上限，已截断: total={}, max={}", rows.size(), EXPORT_MAX_ROWS);
            rows = rows.subList(0, EXPORT_MAX_ROWS);
        }

        String fileName = "商品货位设置_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("商品货位设置");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue((String) EXPORT_COLUMNS[i][0]);
                cell.setCellStyle(headStyle);
            }
            int rowIdx = 1;
            for (ProductLocationVO row : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
                for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                    @SuppressWarnings("unchecked")
                    Function<ProductLocationVO, Object> getter = (Function<ProductLocationVO, Object>) EXPORT_COLUMNS[i][1];
                    writeCell(excelRow.createCell(i), getter.apply(row));
                }
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
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
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.format(DATE_TIME_FORMATTER));
        } else {
            cell.setCellValue(String.valueOf(value));
        }
    }
}
