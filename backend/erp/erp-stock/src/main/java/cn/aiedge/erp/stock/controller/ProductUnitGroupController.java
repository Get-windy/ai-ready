package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.ProductUnitGroupDTO;
import cn.aiedge.erp.stock.dto.ProductUnitGroupVO;
import cn.aiedge.erp.stock.service.ProductUnitGroupService;
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
 * 商品单位组Controller
 *
 * <p>对标 ql361「商品辅助资料 → 商品单位 → 单位组管理」（弹窗）：
 * 工具栏 新增单位组 | 打印(F8) | 导出 | 刷新；查询 单位 | 显示状态；
 * 列 操作 | 单位 | 单位关系。
 */
@Slf4j
@Tag(name = "商品单位组管理")
@RestController
@RequestMapping("/api/erp/product-unit-group")
@RequiredArgsConstructor
public class ProductUnitGroupController {

    private static final int EXPORT_MAX_ROWS = 20000;

    /** 导出列固定为对标数据表列：单位 / 单位关系 */
    private static final String[] EXPORT_COLUMNS = {"单位", "单位关系"};

    private final ProductUnitGroupService productUnitGroupService;

    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    @Operation(summary = "分页查询单位组")
    @SaCheckPermission("product:unit-group:list")
    @GetMapping("/page")
    public Result<IPage<ProductUnitGroupVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productUnitGroupService.getPage(currentTenantId(), keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "单位组详情（含组内单位明细）")
    @SaCheckPermission("product:unit-group:detail")
    @GetMapping("/{id}")
    public Result<ProductUnitGroupVO> detail(@PathVariable Long id) {
        return Result.ok(productUnitGroupService.getDetail(currentTenantId(), id));
    }

    @Operation(summary = "新增单位组")
    @SaCheckPermission("product:unit-group:create")
    @PostMapping
    public Result<Long> create(@RequestBody ProductUnitGroupDTO dto) {
        return Result.ok(productUnitGroupService.createGroup(currentTenantId(), dto));
    }

    @Operation(summary = "修改单位组")
    @SaCheckPermission("product:unit-group:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProductUnitGroupDTO dto) {
        productUnitGroupService.updateGroup(currentTenantId(), id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除单位组")
    @SaCheckPermission("product:unit-group:delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productUnitGroupService.deleteGroup(currentTenantId(), id);
        return Result.ok();
    }

    @Operation(summary = "启用/停用单位组")
    @SaCheckPermission("product:unit-group:update")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productUnitGroupService.updateStatus(currentTenantId(), id, status);
        return Result.ok();
    }

    @Operation(summary = "导出单位组（真实 xlsx）")
    @SaCheckPermission("product:unit-group:export")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        IPage<ProductUnitGroupVO> page =
                productUnitGroupService.getPage(currentTenantId(), keyword, status, 1, EXPORT_MAX_ROWS);
        List<ProductUnitGroupVO> rows = page.getRecords();

        String fileName = "商品单位组_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("商品单位组");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(EXPORT_COLUMNS[i]);
                cell.setCellStyle(headStyle);
            }
            int rowIdx = 1;
            for (ProductUnitGroupVO row : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(row.getUnitNames() == null ? "" : row.getUnitNames());
                excelRow.createCell(1).setCellValue(row.getUnitRates() == null ? "" : row.getUnitRates());
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }
}
