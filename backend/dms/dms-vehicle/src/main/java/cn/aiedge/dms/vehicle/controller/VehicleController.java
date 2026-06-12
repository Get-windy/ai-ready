package cn.aiedge.dms.vehicle.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.vehicle.dto.MaintenanceCreateDTO;
import cn.aiedge.dms.vehicle.dto.VehicleCreateDTO;
import cn.aiedge.dms.vehicle.entity.DmsVehicle;
import cn.aiedge.dms.vehicle.entity.DmsVehicleMaintenance;
import cn.aiedge.dms.vehicle.service.VehicleService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 自有配送车辆管理控制器
 *
 * @author AI-Ready Team
 */
@Tag(name = "自有车辆管理")
@RestController
@RequestMapping("/api/dms/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "分页查询车辆")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<DmsVehicle>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            DmsVehicle query) {
        Page<DmsVehicle> page = new Page<>(pageNum, pageSize);
        return ApiResponse.ok(vehicleService.page(page, query));
    }

    @Operation(summary = "获取车辆详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<DmsVehicle> getDetail(@Parameter(description = "车辆ID") @PathVariable Long id) {
        return ApiResponse.ok(vehicleService.getById(id));
    }

    @Operation(summary = "新增车辆")
    @PostMapping
    @SaCheckPermission("dms:vehicle:create")
    public ApiResponse<DmsVehicle> create(@Parameter(description = "车辆信息") @Valid @RequestBody VehicleCreateDTO dto) {
        return ApiResponse.ok("车辆创建成功", vehicleService.create(dto));
    }

    @Operation(summary = "更新车辆")
    @PutMapping("/{id}")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<DmsVehicle> update(@Parameter(description = "车辆ID") @PathVariable Long id, @Parameter(description = "车辆信息") @Valid @RequestBody VehicleCreateDTO dto) {
        return ApiResponse.ok("车辆更新成功", vehicleService.update(id, dto));
    }

    @Operation(summary = "更新车辆状态")
    @PutMapping("/{id}/status")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<Void> updateStatus(@Parameter(description = "车辆ID") @PathVariable Long id, @Parameter(description = "状态: 0=空闲, 1=使用中, 2=维修中") @RequestParam Integer status) {
        vehicleService.updateStatus(id, status);
        return ApiResponse.ok("状态更新成功", null);
    }

    @Operation(summary = "绑定驾驶员")
    @PostMapping("/{id}/bind-rider")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<Void> bindRider(@Parameter(description = "车辆ID") @PathVariable Long id,
                                        @Parameter(description = "配送员ID") @RequestParam Long riderId,
                                        @Parameter(description = "配送员名称") @RequestParam String riderName) {
        vehicleService.bindRider(id, riderId, riderName);
        return ApiResponse.ok("绑定成功", null);
    }

    @Operation(summary = "更新里程")
    @PutMapping("/{id}/mileage")
    @SaCheckPermission("dms:vehicle:update")
    public ApiResponse<Void> updateMileage(@Parameter(description = "车辆ID") @PathVariable Long id, @Parameter(description = "里程数(km)") @RequestParam Integer mileage) {
        vehicleService.updateMileage(id, mileage);
        return ApiResponse.ok("里程更新成功", null);
    }

    @Operation(summary = "待保养提醒列表")
    @GetMapping("/maintenance-due")
    @SaCheckLogin
    public ApiResponse<Page<DmsVehicle>> maintenanceDue(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "5000") Integer warnKm) {
        Page<DmsVehicle> page = new Page<>(pageNum, pageSize);
        return ApiResponse.ok(vehicleService.pageDueForMaintenance(page, warnKm));
    }

    @Operation(summary = "删除车辆")
    @DeleteMapping("/{id}")
    @SaCheckPermission("dms:vehicle:delete")
    public ApiResponse<Void> delete(@Parameter(description = "车辆ID") @PathVariable Long id) {
        vehicleService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    // ==================== 维保管理 ====================

    @Operation(summary = "分页查询维保记录")
    @GetMapping("/maintenance/page")
    @SaCheckLogin
    public ApiResponse<Page<DmsVehicleMaintenance>> maintenancePage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "车辆ID") @RequestParam(required = false) Long vehicleId) {
        Page<DmsVehicleMaintenance> page = new Page<>(pageNum, pageSize);
        return ApiResponse.ok(vehicleService.pageMaintenance(page, vehicleId));
    }

    @Operation(summary = "新增维保记录")
    @PostMapping("/maintenance")
    @SaCheckPermission("dms:vehicle:maintenance")
    public ApiResponse<DmsVehicleMaintenance> createMaintenance(@Parameter(description = "维保记录信息") @Valid @RequestBody MaintenanceCreateDTO dto) {
        return ApiResponse.ok("维保记录创建成功", vehicleService.createMaintenance(dto));
    }

    @Operation(summary = "删除维保记录")
    @DeleteMapping("/maintenance/{id}")
    @SaCheckPermission("dms:vehicle:maintenance")
    public ApiResponse<Void> deleteMaintenance(@Parameter(description = "维保记录ID") @PathVariable Long id) {
        vehicleService.deleteMaintenance(id);
        return ApiResponse.ok("删除成功", null);
    }
}
