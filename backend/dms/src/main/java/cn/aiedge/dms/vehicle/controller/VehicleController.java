package cn.aiedge.dms.vehicle.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.vehicle.dto.VehicleCertExpiryVO;
import cn.aiedge.dms.vehicle.dto.VehicleDTO;
import cn.aiedge.dms.vehicle.dto.VehicleQueryDTO;
import cn.aiedge.dms.vehicle.service.VehicleService;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 车辆档案控制器（配送 → 人车管理 → 车辆管理，菜单 80770 / `dms:vehicle`）
 *
 * <p>对标《车辆管理开发文档》§3.4 金标准接口清单：</p>
 * <ul>
 *   <li>P0 修复：状态变更统一 <code>PUT /{id}/status</code> + body（原 @RequestParam → 400）；</li>
 *   <li>P0 修复：绑定统一 <code>POST /{id}/bind-rider</code> + body（原路径/参数双不符，另删除不存在的 <code>/{id}/bind</code>）；</li>
 *   <li>P0 修复：批量删除改 <code>POST /batch-delete</code>（原把 "1,2" 拼进 <code>DELETE /{id}</code> → 类型转换失败）；</li>
 *   <li>P0 修复：出参字段统一 <code>currentRider*</code>，页面「当前配送员」列不再恒空；</li>
 *   <li>P1 新增：<code>/options</code> 选择器、<code>/expiring</code> 证件到期清单、<code>/next-code</code>、<code>/unbind-rider</code>、绑定流水。</li>
 * </ul>
 *
 * <p>维保台账已拆分为独立控制器 {@code VehicleMaintenanceController}（/api/dms/vehicle/maintenance/**）。</p>
 *
 * @author AI-Ready Team
 */
@Tag(name = "车辆管理", description = "配送车辆档案（车证/载重/人车绑定/证件到期）")
@Slf4j
@RestController
@RequestMapping("/api/dms/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    // ==================== 查询 ====================

    @Operation(summary = "分页查询车辆（多条件）")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<VehicleDTO>> page(VehicleQueryDTO query) {
        return ApiResponse.ok(vehicleService.page(query));
    }

    @Operation(summary = "车辆下拉选项（调度指派 / 人车绑定 / 线路共用，排除已报废）")
    @GetMapping("/options")
    @SaCheckLogin
    public ApiResponse<List<VehicleDTO>> options() {
        return ApiResponse.ok(vehicleService.options());
    }

    @Operation(summary = "生成下一个车辆编码")
    @GetMapping("/next-code")
    @SaCheckLogin
    public ApiResponse<String> nextCode() {
        return ApiResponse.ok(vehicleService.nextCode());
    }

    @Operation(summary = "证件到期清单（保险 / 年检 / 营运证三证统一视图）")
    @GetMapping("/expiring")
    @SaCheckLogin
    public ApiResponse<List<VehicleCertExpiryVO>> expiring(
            @Parameter(description = "到期窗口天数（含已过期），默认 30") @RequestParam(required = false) Integer days,
            @Parameter(description = "证件类型 INSURANCE / INSPECTION / PERMIT，缺省=全部") @RequestParam(required = false) String certType) {
        return ApiResponse.ok(vehicleService.expiring(days, certType));
    }

    @Operation(summary = "导出车辆（真实 xlsx）")
    @GetMapping("/export")
    @SaCheckLogin
    public void export(VehicleQueryDTO query, HttpServletResponse response) throws IOException {
        List<VehicleDTO> rows = vehicleService.list(query);

        String fileName = "车辆_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {"车牌号", "车辆编码", "车辆类型", "品牌", "型号", "颜色", "VIN", "发动机号",
                "归属类型", "车主", "核定载重(kg)", "核定载客", "货厢容积(m³)", "注册日期",
                "保险到期", "年检到期", "营运证到期", "当前里程(km)", "当前配送员", "车队长", "所属部门", "状态", "备注"};

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("车辆");
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
            for (VehicleDTO dto : rows) {
                Row row = sheet.createRow(rowIdx++);
                String[] values = {
                        nullSafe(dto.getPlateNo()), nullSafe(dto.getVehicleCode()), nullSafe(dto.getVehicleTypeText()),
                        nullSafe(dto.getBrand()), nullSafe(dto.getModel()), nullSafe(dto.getColor()),
                        nullSafe(dto.getVin()), nullSafe(dto.getEngineNo()), nullSafe(dto.getOwnershipTypeText()),
                        nullSafe(dto.getOwnerName()),
                        dto.getRatedLoad() == null ? "" : dto.getRatedLoad().stripTrailingZeros().toPlainString(),
                        dto.getRatedPassenger() == null ? "" : String.valueOf(dto.getRatedPassenger()),
                        dto.getCargoVolume() == null ? "" : dto.getCargoVolume().stripTrailingZeros().toPlainString(),
                        nullSafe(dateText(dto.getRegisterDate())), nullSafe(dateText(dto.getInsuranceExpireDate())),
                        nullSafe(dateText(dto.getInspectionExpireDate())), nullSafe(dateText(dto.getOperatingPermitExpireDate())),
                        dto.getCurrentMileage() == null ? "" : String.valueOf(dto.getCurrentMileage()),
                        nullSafe(dto.getCurrentRiderName()), nullSafe(dto.getVehicleManagerName()),
                        nullSafe(dto.getDepartment()), nullSafe(dto.getStatusText()), nullSafe(dto.getRemark()),
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

    @Operation(summary = "获取车辆详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<VehicleDTO> getDetail(@Parameter(description = "车辆ID") @PathVariable Long id) {
        return ApiResponse.ok(vehicleService.getById(id));
    }

    // ==================== 写操作 ====================

    @Operation(summary = "新增车辆")
    @PostMapping
    @SaCheckPermission("dms:vehicle:create")
    public ApiResponse<VehicleDTO> create(@Parameter(description = "车辆信息") @Valid @RequestBody VehicleDTO dto) {
        return ApiResponse.ok("车辆创建成功", vehicleService.create(dto));
    }

    @Operation(summary = "修改车辆")
    @PutMapping("/{id}")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<VehicleDTO> update(@Parameter(description = "车辆ID") @PathVariable Long id,
                                          @Parameter(description = "车辆信息") @Valid @RequestBody VehicleDTO dto) {
        return ApiResponse.ok("车辆更新成功", vehicleService.update(id, dto));
    }

    @Operation(summary = "删除车辆")
    @DeleteMapping("/{id}")
    @SaCheckPermission("dms:vehicle:delete")
    public ApiResponse<Void> delete(@Parameter(description = "车辆ID") @PathVariable Long id) {
        vehicleService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @Operation(summary = "批量删除车辆")
    @PostMapping("/batch-delete")
    @SaCheckPermission("dms:vehicle:delete")
    public ApiResponse<Integer> batchDelete(@RequestBody Map<String, List<Object>> body) {
        return ApiResponse.ok("删除成功", vehicleService.batchDelete(toIdList(body)));
    }

    @Operation(summary = "状态变更（状态机校验：有在途任务不可维修/报废，报废不可绑定）")
    @PutMapping("/{id}/status")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<VehicleDTO> updateStatus(@Parameter(description = "车辆ID") @PathVariable Long id,
                                                @RequestBody VehicleStatusRequest body) {
        return ApiResponse.ok("状态更新成功", vehicleService.updateStatus(id, body == null ? null : body.status()));
    }

    @Operation(summary = "批量状态变更（批量启停/批量报废）")
    @PostMapping("/batch-status")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<Integer> batchStatus(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIdList(body);
        Integer status = body.get("status") == null ? null : Integer.valueOf(String.valueOf(body.get("status")));
        return ApiResponse.ok("操作成功", vehicleService.batchStatus(ids, status));
    }

    @Operation(summary = "绑定配送员（人车一对一，写绑定流水）")
    @PostMapping("/{id}/bind-rider")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<VehicleDTO> bindRider(@Parameter(description = "车辆ID") @PathVariable Long id,
                                             @RequestBody BindRiderRequest body) {
        return ApiResponse.ok("绑定成功",
                vehicleService.bindRider(id, body == null ? null : body.riderId(),
                        body == null ? null : body.mileage(), body == null ? null : body.remark()));
    }

    @Operation(summary = "解绑配送员（结对绑定流水：交车时间/里程）")
    @PostMapping("/{id}/unbind-rider")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<VehicleDTO> unbindRider(@Parameter(description = "车辆ID") @PathVariable Long id,
                                               @RequestBody(required = false) UnbindRiderRequest body) {
        return ApiResponse.ok("解绑成功", vehicleService.unbindRider(id, body == null ? null : body.mileage()));
    }

    @Operation(summary = "绑定/解绑流水")
    @GetMapping("/{id}/binding-history")
    @SaCheckLogin
    public ApiResponse<List<DmsRiderVehicleBinding>> bindingHistory(@Parameter(description = "车辆ID") @PathVariable Long id) {
        return ApiResponse.ok(vehicleService.bindingHistory(id));
    }

    @Operation(summary = "更新当前里程")
    @PutMapping("/{id}/mileage")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<Void> updateMileage(@Parameter(description = "车辆ID") @PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        vehicleService.updateMileage(id, body.get("mileage") == null ? null : Integer.valueOf(String.valueOf(body.get("mileage"))));
        return ApiResponse.ok("里程更新成功", null);
    }

    @Operation(summary = "待保养/证件到期提醒列表")
    @GetMapping("/maintenance-due")
    @SaCheckLogin
    public ApiResponse<Page<VehicleDTO>> maintenanceDue(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "5000") Integer warnKm) {
        return ApiResponse.ok(vehicleService.pageDueForMaintenance(new Page<>(pageNum, pageSize), warnKm));
    }

    // ==================== 请求体 / 工具 ====================

    @Schema(description = "状态变更请求")
    public record VehicleStatusRequest(Integer status) {
    }

    @Schema(description = "绑定配送员请求")
    public record BindRiderRequest(Long riderId, Integer mileage, String remark) {
    }

    @Schema(description = "解绑配送员请求")
    public record UnbindRiderRequest(Integer mileage) {
    }

    @SuppressWarnings("unchecked")
    private List<Long> toIdList(Map<String, ?> body) {
        Object raw = body == null ? null : body.get("ids");
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return ((List<Object>) list).stream()
                .filter(java.util.Objects::nonNull)
                .map(v -> Long.valueOf(String.valueOf(v)))
                .toList();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String dateText(LocalDate date) {
        return date == null ? null : date.toString();
    }
}
