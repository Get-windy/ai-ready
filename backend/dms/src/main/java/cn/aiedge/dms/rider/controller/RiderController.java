package cn.aiedge.dms.rider.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.rider.dto.RiderQuery;
import cn.aiedge.dms.rider.dto.RiderVO;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.service.RiderService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 配送员管理控制器（金标准）
 *
 * <p>接口口径见《配送员管理开发文档》§3.3：状态更新 PUT + body、审核收 remark、
 * 删除走 DELETE、位置上报 body 携带 riderId、统一 /options 选择器。</p>
 */
@Tag(name = "配送员管理")
@RestController
@RequestMapping("/api/dms/rider")
@RequiredArgsConstructor
public class RiderController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RiderService riderService;

    @Operation(summary = "多条件分页查询配送员")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<RiderVO>> page(RiderQuery query) {
        return ApiResponse.ok(riderService.pageQuery(query));
    }

    @Operation(summary = "配送员列表（不分页，导出/兼容旧调用）")
    @GetMapping("/list")
    @SaCheckLogin
    public ApiResponse<List<RiderVO>> list(RiderQuery query) {
        return ApiResponse.ok(riderService.list(query));
    }

    @Operation(summary = "配送员选择器数据源")
    @GetMapping("/options")
    @SaCheckLogin
    public ApiResponse<List<Map<String, Object>>> options(
            @Parameter(description = "关键字（编号/姓名/手机）") @RequestParam(required = false) String keyword,
            @Parameter(description = "仅可指派（审核通过且资质未过期）") @RequestParam(defaultValue = "false") boolean assignable) {
        return ApiResponse.ok(riderService.options(keyword, assignable));
    }

    @Operation(summary = "生成下一个配送员编号")
    @GetMapping("/next-code")
    @SaCheckLogin
    public ApiResponse<String> nextCode() {
        return ApiResponse.ok(riderService.nextCode());
    }

    @Operation(summary = "获取配送员详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<RiderVO> getDetail(@Parameter(description = "配送员ID") @PathVariable Long id) {
        return ApiResponse.ok(riderService.getDetail(id));
    }

    @Operation(summary = "新增配送员")
    @PostMapping
    @SaCheckPermission("dms:rider:create")
    public ApiResponse<RiderVO> create(@Parameter(description = "配送员信息") @RequestBody DmsRider rider) {
        return ApiResponse.ok("创建成功", riderService.create(rider));
    }

    @Operation(summary = "修改配送员")
    @PutMapping("/{id}")
    @SaCheckPermission("dms:rider:update")
    public ApiResponse<RiderVO> update(@Parameter(description = "配送员ID") @PathVariable Long id,
                                       @Parameter(description = "配送员信息") @RequestBody DmsRider rider) {
        return ApiResponse.ok("更新成功", riderService.update(id, rider));
    }

    @Operation(summary = "更新接单状态（状态机 + 资质门控）")
    @PutMapping("/{id}/status")
    @SaCheckPermission("dms:rider:update")
    public ApiResponse<Void> updateStatus(@Parameter(description = "配送员ID") @PathVariable Long id,
                                          @Parameter(description = "状态: 0=离线 1=空闲 2=忙碌 3=休息") @RequestBody StatusRequest request) {
        riderService.updateStatus(id, request.status());
        return ApiResponse.ok("状态更新成功", null);
    }

    @Operation(summary = "批量启停（批量更新状态）")
    @PostMapping("/batch-status")
    @SaCheckPermission("dms:rider:update")
    public ApiResponse<Integer> batchStatus(@RequestBody BatchStatusRequest request) {
        return ApiResponse.ok("批量操作成功", riderService.batchUpdateStatus(request.ids(), request.status()));
    }

    @Operation(summary = "审核配送员（通过/拒绝 + 备注）")
    @PostMapping("/{id}/approve")
    @SaCheckPermission("dms:rider:approve")
    public ApiResponse<Void> approve(@Parameter(description = "配送员ID") @PathVariable Long id,
                                     @RequestBody ApproveRequest request) {
        riderService.approve(id, request.verifyStatus(), request.remark());
        return ApiResponse.ok(request.verifyStatus() != null && request.verifyStatus() == 1 ? "审核通过" : "审核拒绝", null);
    }

    @Operation(summary = "批量审核")
    @PostMapping("/batch-approve")
    @SaCheckPermission("dms:rider:approve")
    public ApiResponse<Integer> batchApprove(@RequestBody BatchApproveRequest request) {
        int count = riderService.batchApprove(request.ids(), request.verifyStatus(), request.remark());
        return ApiResponse.ok("批量审核完成", count);
    }

    @Operation(summary = "位置上报（配送员端调用）")
    @PostMapping("/location")
    @SaCheckLogin
    public ApiResponse<Void> reportLocation(@Parameter(description = "位置信息") @RequestBody LocationRequest request) {
        riderService.updateLocation(request.riderId(), request.lat(), request.lng());
        return ApiResponse.ok("位置上报成功", null);
    }

    @Operation(summary = "删除配送员")
    @DeleteMapping("/{id}")
    @SaCheckPermission("dms:rider:delete")
    public ApiResponse<Void> delete(@Parameter(description = "配送员ID") @PathVariable Long id) {
        riderService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @Operation(summary = "批量删除配送员")
    @PostMapping("/batch-delete")
    @SaCheckPermission("dms:rider:delete")
    public ApiResponse<Integer> batchDelete(@RequestBody BatchIdsRequest request) {
        return ApiResponse.ok("批量删除成功", riderService.batchDelete(request.ids()));
    }

    @Operation(summary = "下载配送员导入模板")
    @GetMapping("/import-template")
    @SaCheckLogin
    public void importTemplate(HttpServletResponse response) throws IOException {
        String[] headers = {"导入结果", "配送员编号", "姓名", "手机号", "配送员类型", "归属渠道名称",
                "平台骑手ID", "身份证号", "驾驶证号", "健康证号", "资质到期日期", "入职日期", "部门名称", "车牌号", "结算方式", "备注"};
        String[] comments = {
                "由系统在导入后回写结果，请勿填写",
                "唯一编号（PSY0001）；留空则自动生成，重号将被拒绝",
                "必填",
                "必填，11 位手机号",
                "必填：企业员工 / 众包兼职 / 外部平台配送员 / 社会车辆司机",
                "外部平台配送员必填，需与《渠道管理》中的渠道名称一致",
                "外部平台配送员必填",
                "身份证号（列表展示自动脱敏）",
                "驾驶证号，选填",
                "健康证号，选填",
                "资质/证照有效期，格式 yyyy-MM-dd；到期后不可接单",
                "入职日期（企业员工），格式 yyyy-MM-dd",
                "企业员工填部门名称（不存在则忽略）",
                "社会车辆司机必填",
                "按单结算 / 月结 / 时段结算",
                "备注信息"
        };
        String fileName = "配送员导入模板_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("配送员信息");
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

    @Operation(summary = "Excel 导入配送员（真实落库）")
    @PostMapping("/import-excel")
    @SaCheckPermission("dms:rider:create")
    public ApiResponse<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("导入完成", riderService.importExcel(file));
    }

    @Operation(summary = "导出配送员（真实 xlsx）")
    @GetMapping("/export")
    @SaCheckLogin
    public void export(RiderQuery query, HttpServletResponse response) throws IOException {
        List<RiderVO> rows = riderService.list(query);
        String fileName = "配送员_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {"配送员编号", "姓名", "手机号", "类型", "归属", "状态", "在线状态", "审核状态",
                "资质到期", "评分", "累计单量", "今日单量", "准时率(%)", "当前车辆", "最近上报时间", "备注", "创建时间"};

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("配送员");
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
            for (RiderVO vo : rows) {
                Row row = sheet.createRow(rowIdx++);
                String belong = vo.getChannelName() != null ? vo.getChannelName()
                        : (vo.getUserName() != null ? vo.getUserName() : "");
                String[] values = {
                        nullSafe(vo.getRiderNo()),
                        nullSafe(vo.getRealName()),
                        nullSafe(vo.getPhone()),
                        nullSafe(vo.getRiderTypeText()),
                        belong,
                        nullSafe(vo.getStatusText()),
                        nullSafe(vo.getOnlineStatusText()),
                        nullSafe(vo.getVerifyStatusText()),
                        vo.getQualificationExpireDate() == null ? "" : vo.getQualificationExpireDate().toString(),
                        vo.getRatingScore() == null ? "" : vo.getRatingScore().toPlainString(),
                        vo.getTotalOrders() == null ? "0" : String.valueOf(vo.getTotalOrders()),
                        vo.getTodayOrders() == null ? "0" : String.valueOf(vo.getTodayOrders()),
                        vo.getPunctualRate() == null ? "" : vo.getPunctualRate().toPlainString(),
                        nullSafe(vo.getVehiclePlate()),
                        vo.getLastReportTime() == null ? "" : DT.format(vo.getLastReportTime()),
                        nullSafe(vo.getRemark()),
                        vo.getCreateTime() == null ? "" : DT.format(vo.getCreateTime()),
                };
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            workbook.write(response.getOutputStream());
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    // ── 请求体 ──

    public record StatusRequest(Integer status) {
    }

    public record ApproveRequest(Integer verifyStatus, String remark) {
    }

    public record BatchApproveRequest(List<Long> ids, Integer verifyStatus, String remark) {
    }

    public record BatchStatusRequest(List<Long> ids, Integer status) {
    }

    public record BatchIdsRequest(List<Long> ids) {
    }

    public record LocationRequest(Long riderId, BigDecimal lat, BigDecimal lng) {
    }
}
