package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.MallTagVO;
import cn.aiedge.erp.stock.entity.MallTag;
import cn.aiedge.erp.stock.service.MallTagService;
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
 * 商城标签Controller
 *
 * <p>对标 ql361「商品辅助资料 → 商品标签」。
 */
@Slf4j
@Tag(name = "商城标签管理")
@RestController
@RequestMapping("/api/erp/mall-tag")
@RequiredArgsConstructor
public class MallTagController {

    private static final int EXPORT_MAX_ROWS = 20000;

    private static final String[][] EXPORT_COLUMNS = {
            {"tagName", "标签名称"},
            {"productNames", "对应商品"},
    };

    private final MallTagService mallTagService;

    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    @Operation(summary = "获取所有标签（当前租户，含标准槽位初始化）")
    @SaCheckPermission("mall:tag:list")
    @GetMapping("/list")
    public Result<List<MallTag>> list() {
        // 标签是「标准槽位 TAG_1..TAG_20 + 用户自定义昵称」，租户首次访问时补齐槽位
        return Result.ok(mallTagService.ensureStandardSlots(currentTenantId()));
    }

    @Operation(summary = "分页查询标签（含「对应商品」聚合）")
    @SaCheckPermission("mall:tag:list")
    @GetMapping("/page")
    public Result<IPage<MallTagVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(mallTagService.getPage(currentTenantId(), keyword, pageNum, pageSize));
    }

    @Operation(summary = "创建标签（槽位编码由系统按 TAG_N 递增分配）")
    @SaCheckPermission("mall:tag:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody MallTag tag) {
        tag.setTenantId(currentTenantId());
        return Result.ok(mallTagService.createTag(tag));
    }

    @Operation(summary = "更新标签")
    @SaCheckPermission("mall:tag:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody MallTag tag) {
        tag.setId(id);
        return Result.ok(mallTagService.updateById(tag));
    }

    @Operation(summary = "启用/停用标签")
    @SaCheckPermission("mall:tag:update")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        mallTagService.updateStatus(currentTenantId(), id, status);
        return Result.ok();
    }

    @Operation(summary = "删除标签")
    @SaCheckPermission("mall:tag:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(mallTagService.removeById(id));
    }

    @Operation(summary = "导出标签（真实 Excel 流）")
    @SaCheckPermission("mall:tag:export")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       HttpServletResponse response) throws IOException {
        IPage<MallTagVO> page = mallTagService.getPage(currentTenantId(), keyword, 1, EXPORT_MAX_ROWS);
        List<MallTagVO> rows = page.getRecords();
        if (rows.size() > EXPORT_MAX_ROWS) {
            rows = rows.subList(0, EXPORT_MAX_ROWS);
        }

        String fileName = "商品标签_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("商品标签");
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
            for (MallTagVO row : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
                excelRow.createCell(0).setCellValue(row.getTagName() == null ? "" : row.getTagName());
                excelRow.createCell(1).setCellValue(row.getProductNames() == null ? "" : row.getProductNames());
            }
            for (int i = 0; i < EXPORT_COLUMNS.length; i++) {
                sheet.setColumnWidth(i, 24 * 256);
            }
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        }
    }
}
