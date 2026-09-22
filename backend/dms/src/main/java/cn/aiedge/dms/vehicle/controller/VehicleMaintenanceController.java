package cn.aiedge.dms.vehicle.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.vehicle.dto.MaintenanceCreateDTO;
import cn.aiedge.dms.vehicle.dto.MaintenanceQuery;
import cn.aiedge.dms.vehicle.dto.MaintenanceVO;
import cn.aiedge.dms.vehicle.dto.VendorOptionVO;
import cn.aiedge.dms.vehicle.service.VehicleMaintenanceService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 车辆维保记录控制器（配送 → 人车管理 → 车辆维护）
 *
 * <p>统一前缀 <code>/api/dms/vehicle/maintenance</code>；原实现仅 page/create/delete 三个端点，
 * 本次补齐详情 / 修改 / 号段 / 统计 / 到期提醒 / 真实 xlsx 导出。</p>
 *
 * @author AI-Ready Team
 */
@Tag(name = "车辆维保", description = "车辆保养/维修/年检/保险/事故台账")
@RestController
@RequestMapping("/api/dms/vehicle/maintenance")
@RequiredArgsConstructor
@SaCheckLogin
public class VehicleMaintenanceController {

    private final VehicleMaintenanceService maintenanceService;

    @Operation(summary = "分页查询维保记录")
    @SaCheckPermission("dms:vehicle-maintenance:list")
    @GetMapping("/page")
    public ApiResponse<Page<MaintenanceVO>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Integer pageSize,
            MaintenanceQuery query) {
        return ApiResponse.ok(maintenanceService.page(query, pageNum, pageSize));
    }

    @Operation(summary = "获取维保记录详情")
    @SaCheckPermission("dms:vehicle-maintenance:detail")
    @GetMapping("/{id}")
    public ApiResponse<MaintenanceVO> detail(@Parameter(description = "维保记录ID") @PathVariable Long id) {
        return ApiResponse.ok(maintenanceService.detail(id));
    }

    @Operation(summary = "生成下一个维保单号")
    @SaCheckPermission("dms:vehicle-maintenance:list")
    @GetMapping("/next-no")
    public ApiResponse<String> nextNo() {
        return ApiResponse.ok(maintenanceService.nextNo());
    }

    @Operation(summary = "维保厂商选择器（往来单位：供应商/其他往来单位）")
    @SaCheckPermission("dms:vehicle-maintenance:view")
    @GetMapping("/vendor-options")
    public ApiResponse<List<VendorOptionVO>> vendorOptions(
            @Parameter(description = "名称/编码/助记码关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "返回条数（缺省20，上限50）") @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(maintenanceService.vendorOptions(keyword, limit));
    }

    @Operation(summary = "维保费用统计（按类型/月份/车辆/厂商）")
    @SaCheckPermission("dms:vehicle-maintenance:view")
    @GetMapping("/stat")
    public ApiResponse<Map<String, Object>> stat(MaintenanceQuery query) {
        return ApiResponse.ok(maintenanceService.stat(query));
    }

    @Operation(summary = "维保到期提醒（日期 + 里程双阈值）")
    @SaCheckPermission("dms:vehicle-maintenance:view")
    @GetMapping("/expiring")
    public ApiResponse<List<MaintenanceVO>> expiring(
            @Parameter(description = "日期预警窗口(天)") @RequestParam(defaultValue = "30") Integer days,
            @Parameter(description = "里程预警窗口(公里)") @RequestParam(defaultValue = "1000") Integer warnKm) {
        return ApiResponse.ok(maintenanceService.expiring(days, warnKm));
    }

    @Operation(summary = "新增维保记录")
    @SaCheckPermission("dms:vehicle-maintenance:create")
    @PostMapping
    public ApiResponse<MaintenanceVO> create(@Valid @RequestBody MaintenanceCreateDTO dto) {
        return ApiResponse.ok("维保记录创建成功", maintenanceService.create(dto));
    }

    @Operation(summary = "修改维保记录")
    @SaCheckPermission("dms:vehicle-maintenance:update")
    @PutMapping("/{id}")
    public ApiResponse<MaintenanceVO> update(@Parameter(description = "维保记录ID") @PathVariable Long id,
                                             @Valid @RequestBody MaintenanceCreateDTO dto) {
        return ApiResponse.ok("维保记录修改成功", maintenanceService.update(id, dto));
    }

    @Operation(summary = "删除维保记录")
    @SaCheckPermission("dms:vehicle-maintenance:delete")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "维保记录ID") @PathVariable Long id) {
        maintenanceService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    @Operation(summary = "导出维保记录 xlsx")
    @SaCheckPermission("dms:vehicle-maintenance:export")
    @GetMapping("/export")
    public void export(MaintenanceQuery query, HttpServletResponse response) throws IOException {
        maintenanceService.export(query, response);
    }
}
