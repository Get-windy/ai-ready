package cn.aiedge.erp.delivery.route.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.delivery.route.dto.RouteDTO;
import cn.aiedge.erp.delivery.route.dto.RouteQueryDTO;
import cn.aiedge.erp.delivery.route.service.RouteMasterService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 线路主数据 Controller（资料 → 配送管理 → 线路）
 *
 * 对标 ql361：工具栏 新增 / 导入 / 刷新 / 打印(F8) / 导出；查询项 线路编号·线路名称·配送区域 + 显示状态(默认已启用) + 线路类型(默认全部)；
 * 列表 6 列（线路编号/线路名称/线路类型/物流公司/配送区域/备注）；新增弹窗「配送路线」。
 *
 * ⚠️ 与《配送路线单》执行单据（/api/delivery/route，DeliveryRouteController）严格区分：本页是线路档案主数据。
 */
@Tag(name = "线路主数据", description = "线路档案（资料 → 配送管理 → 线路）增删改查、启停、导入导出")
@Slf4j
@RestController
@RequestMapping("/api/erp/md/route")
@RequiredArgsConstructor
public class RouteMasterController {

    private final RouteMasterService routeMasterService;

    @Operation(summary = "分页查询线路")
    @SaCheckPermission("md:route-master:list")
    @GetMapping("/page")
    @OperationLog(module = "线路", type = "QUERY", desc = "分页查询线路")
    public ApiResponse<Page<RouteDTO>> page(RouteQueryDTO query) {
        return ApiResponse.success(routeMasterService.page(query));
    }

    @Operation(summary = "查询线路详情（含配送区域子表）")
    @SaCheckPermission("md:route-master:detail")
    @GetMapping("/{id}")
    @OperationLog(module = "线路", type = "QUERY", desc = "查询线路详情")
    public ApiResponse<RouteDTO> detail(@PathVariable Long id) {
        return ApiResponse.success(routeMasterService.getById(id));
    }

    @Operation(summary = "启用线路下拉（供销售订单/出库单「配送线路」引用）")
    @GetMapping("/options")
    @OperationLog(module = "线路", type = "QUERY", desc = "查询线路下拉")
    public ApiResponse<List<RouteDTO>> options() {
        return ApiResponse.success(routeMasterService.options());
    }

    @Operation(summary = "生成下一个线路编号")
    @SaCheckPermission("md:route-master:view")
    @GetMapping("/next-code")
    @OperationLog(module = "线路", type = "QUERY", desc = "生成下一个线路编号")
    public ApiResponse<String> nextCode() {
        return ApiResponse.success(routeMasterService.nextCode());
    }

    @Operation(summary = "新增线路")
    @SaCheckPermission("md:route-master:create")
    @PostMapping
    @OperationLog(module = "线路", type = "CREATE", desc = "新增线路")
    public ApiResponse<RouteDTO> create(@RequestBody RouteDTO dto) {
        return ApiResponse.success("新增成功", routeMasterService.create(dto));
    }

    @Operation(summary = "修改线路")
    @SaCheckPermission("md:route-master:update")
    @PutMapping("/{id}")
    @OperationLog(module = "线路", type = "UPDATE", desc = "修改线路")
    public ApiResponse<RouteDTO> update(@PathVariable Long id, @RequestBody RouteDTO dto) {
        return ApiResponse.success("修改成功", routeMasterService.update(id, dto));
    }

    @Operation(summary = "删除线路")
    @SaCheckPermission("md:route-master:delete")
    @DeleteMapping("/{id}")
    @OperationLog(module = "线路", type = "DELETE", desc = "删除线路")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        routeMasterService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @Operation(summary = "启用/停用线路")
    @SaCheckPermission("md:route-master:status")
    @PutMapping("/{id}/status")
    @OperationLog(module = "线路", type = "UPDATE", desc = "启用/停用线路")
    public ApiResponse<RouteDTO> updateStatus(
            @PathVariable Long id,
            @Parameter(description = "状态 ENABLED-启用 / DISABLED-停用") @RequestBody Map<String, String> body) {
        return ApiResponse.success(routeMasterService.updateStatus(id, body == null ? null : body.get("status")));
    }

    @Operation(summary = "批量启用/停用线路")
    @SaCheckPermission("md:route-master:status")
    @PostMapping("/batch-status")
    @OperationLog(module = "线路", type = "UPDATE", desc = "批量启用/停用线路")
    public ApiResponse<Integer> batchStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("ids");
        List<Long> ids = rawIds == null ? List.of()
                : rawIds.stream().map(v -> Long.valueOf(String.valueOf(v))).toList();
        String status = body.get("status") == null ? null : String.valueOf(body.get("status"));
        return ApiResponse.success("操作成功", routeMasterService.batchStatus(ids, status));
    }

    @Operation(summary = "下载线路导入模板（对标「基本信息导入」向导第 1 步）")
    @SaCheckPermission("md:route-master:view")
    @GetMapping("/import-template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        String[] headers = {"导入结果", "线路编号", "线路名称(必填)", "线路类型(自配/物流)", "物流公司", "配送区域编码", "备注"};
        String[] comments = {
                "由系统在导入后回写结果，请勿填写",
                "唯一编号；留空则按「XL001」规则自动生成，重号将被拒绝",
                "必填，不可为空",
                "填写「自配」或「物流」（二选一，不可同时填写）",
                "线路类型含「物流」时填写承运物流公司名称",
                "行政区划编码，多个以英文逗号分隔（如 110101,120101）",
                "备注信息"
        };
        String fileName = "线路导入模板_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("线路信息");
            CellStyle headStyle = workbook.createCellStyle();
            Font headFont = workbook.createFont();
            headFont.setBold(true);
            headStyle.setFont(headFont);

            Row head = sheet.createRow(0);
            org.apache.poi.xssf.usermodel.XSSFDrawing drawing =
                    ((org.apache.poi.xssf.usermodel.XSSFSheet) sheet).createDrawingPatriarch();
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
                org.apache.poi.xssf.usermodel.XSSFClientAnchor anchor =
                        new org.apache.poi.xssf.usermodel.XSSFClientAnchor(0, 0, 0, 0, i, 0, i + 2, 3);
                org.apache.poi.xssf.usermodel.XSSFComment comment = drawing.createCellComment(anchor);
                comment.setString(new org.apache.poi.xssf.usermodel.XSSFRichTextString(comments[i]));
                comment.setAuthor("系统");
                cell.setCellComment(comment);
                sheet.setColumnWidth(i, 22 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    @Operation(summary = "Excel 导入线路（真实落库）")
    @SaCheckPermission("md:route-master:import")
    @PostMapping("/import-excel")
    @OperationLog(module = "线路", type = "CREATE", desc = "Excel 导入线路")
    public ApiResponse<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("导入完成", routeMasterService.importExcel(file));
    }

    @Operation(summary = "导出线路（真实 xlsx）")
    @SaCheckPermission("md:route-master:export")
    @GetMapping("/export")
    @OperationLog(module = "线路", type = "QUERY", desc = "导出线路")
    public void export(RouteQueryDTO query, HttpServletResponse response) throws IOException {
        List<RouteDTO> rows = routeMasterService.list(query);

        String fileName = "线路_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {"线路编号", "线路名称", "线路类型", "物流公司", "配送区域", "备注", "显示状态"};

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("线路");
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
            for (RouteDTO dto : rows) {
                Row row = sheet.createRow(rowIdx++);
                String[] values = {
                        nullSafe(dto.getRouteCode()),
                        nullSafe(dto.getRouteName()),
                        nullSafe(dto.getRouteTypeText()),
                        nullSafe(dto.getExpressName()),
                        nullSafe(dto.getAreaText()),
                        nullSafe(dto.getRemark()),
                        nullSafe(dto.getStatusText()),
                };
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
