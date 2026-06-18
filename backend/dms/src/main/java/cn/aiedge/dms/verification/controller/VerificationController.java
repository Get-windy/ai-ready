package cn.aiedge.dms.verification.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.verification.dto.BindingCreateDTO;
import cn.aiedge.dms.verification.dto.VehicleInspectionCreateDTO;
import cn.aiedge.dms.verification.entity.DmsPositionVerification;
import cn.aiedge.dms.verification.entity.DmsRiderVehicleBinding;
import cn.aiedge.dms.verification.entity.DmsVerificationAlert;
import cn.aiedge.dms.verification.enums.BindingStatusEnum;
import cn.aiedge.dms.verification.mapper.DmsRiderVehicleBindingMapper;
import cn.aiedge.dms.verification.service.VerificationService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 人车绑定与核验控制器
 *
 * 提供出车验车、人车绑定、位置核验、告警管理等REST接口。
 *
 * @author AI-Ready Team
 */
@Tag(name = "人车绑定与核验")
@RestController
@RequestMapping("/api/dms/verification")
@RequiredArgsConstructor
@SaCheckLogin
public class VerificationController {

    private final VerificationService verificationService;
    private final DmsRiderVehicleBindingMapper bindingMapper;

    // ==================== 出车验车 ====================

    @Operation(summary = "创建验车记录")
    @PostMapping("/inspection")
    @SaCheckPermission("dms:verification:operate")
    public ApiResponse<Long> createInspection(
            @Valid @RequestBody VehicleInspectionCreateDTO dto) {
        long riderId = StpUtil.getLoginIdAsLong();
        Long id = verificationService.createInspection(dto, riderId);
        return ApiResponse.success(id);
    }

    // ==================== 人车绑定 ====================

    @Operation(summary = "创建人车绑定")
    @PostMapping("/bind")
    @SaCheckPermission("dms:verification:operate")
    public ApiResponse<Long> bind(
            @Valid @RequestBody BindingCreateDTO dto) {
        Long id = verificationService.bind(dto);
        return ApiResponse.success(id);
    }

    @Operation(summary = "交车解绑")
    @PostMapping("/{id}/handover")
    @SaCheckPermission("dms:verification:operate")
    public ApiResponse<Void> handover(
            @Parameter(description = "绑定记录ID") @PathVariable Long id,
            @Parameter(description = "交车里程") @RequestParam(required = false) Integer handoverMileage,
            @Parameter(description = "交车位置纬度") @RequestParam(required = false) BigDecimal handoverLat,
            @Parameter(description = "交车位置经度") @RequestParam(required = false) BigDecimal handoverLng) {
        verificationService.handover(id, handoverMileage, handoverLat, handoverLng);
        return ApiResponse.success();
    }

    // ==================== 绑定查询 ====================

    @Operation(summary = "分页查询绑定记录")
    @GetMapping("/binding/page")
    public ApiResponse<IPage<DmsRiderVehicleBinding>> pageBindings(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        IPage<DmsRiderVehicleBinding> result = verificationService.pageBindings(page, size, tenantId, status);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取配送员活跃绑定")
    @GetMapping("/binding/active/rider/{riderId}")
    public ApiResponse<DmsRiderVehicleBinding> getActiveBindingByRider(
            @Parameter(description = "配送员ID") @PathVariable Long riderId) {
        DmsRiderVehicleBinding binding = bindingMapper.selectOne(
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .eq(DmsRiderVehicleBinding::getRiderId, riderId)
                        .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
                        .last("LIMIT 1")
        );
        return ApiResponse.success(binding);
    }

    @Operation(summary = "获取车辆活跃绑定")
    @GetMapping("/binding/active/vehicle/{vehicleId}")
    public ApiResponse<DmsRiderVehicleBinding> getActiveBindingByVehicle(
            @Parameter(description = "车辆ID") @PathVariable Long vehicleId) {
        DmsRiderVehicleBinding binding = bindingMapper.selectOne(
                new LambdaQueryWrapper<DmsRiderVehicleBinding>()
                        .eq(DmsRiderVehicleBinding::getVehicleId, vehicleId)
                        .eq(DmsRiderVehicleBinding::getStatus, BindingStatusEnum.ACTIVE.getValue())
                        .last("LIMIT 1")
        );
        return ApiResponse.success(binding);
    }

    // ==================== 位置核验 ====================

    @Operation(summary = "手动位置核验")
    @PostMapping("/verify/{bindingId}")
    @SaCheckPermission("dms:verification:operate")
    public ApiResponse<Long> verifyPosition(
            @Parameter(description = "绑定记录ID") @PathVariable Long bindingId,
            @Parameter(description = "配送员纬度") @RequestParam BigDecimal riderLat,
            @Parameter(description = "配送员经度") @RequestParam BigDecimal riderLng,
            @Parameter(description = "配送员上报时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime riderReportTime,
            @Parameter(description = "车辆纬度") @RequestParam BigDecimal vehicleLat,
            @Parameter(description = "车辆经度") @RequestParam BigDecimal vehicleLng,
            @Parameter(description = "车辆上报时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime vehicleReportTime,
            @Parameter(description = "偏差阈值(米)") @RequestParam(required = false) BigDecimal threshold) {
        Long id = verificationService.verifyPosition(bindingId, riderLat, riderLng, riderReportTime,
                vehicleLat, vehicleLng, vehicleReportTime, threshold);
        return ApiResponse.success(id);
    }

    // ==================== 核验记录查询 ====================

    @Operation(summary = "分页查询核验记录")
    @GetMapping("/verify/page")
    public ApiResponse<IPage<DmsPositionVerification>> pageVerifications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "绑定记录ID") @RequestParam(required = false) Long bindingId) {
        IPage<DmsPositionVerification> result = verificationService.pageVerifications(page, size, bindingId);
        return ApiResponse.success(result);
    }

    // ==================== 告警管理 ====================

    @Operation(summary = "分页查询告警记录")
    @GetMapping("/alert/page")
    public ApiResponse<IPage<DmsVerificationAlert>> pageAlerts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "租户ID") @RequestParam(required = false) Long tenantId,
            @Parameter(description = "告警类型") @RequestParam(required = false) Integer alertType,
            @Parameter(description = "处理状态") @RequestParam(required = false) Integer handleStatus) {
        IPage<DmsVerificationAlert> result = verificationService.pageAlerts(page, size, tenantId, alertType, handleStatus);
        return ApiResponse.success(result);
    }

    @Operation(summary = "处理告警")
    @PutMapping("/alert/{id}/handle")
    @SaCheckPermission("dms:verification:operate")
    public ApiResponse<Void> handleAlert(
            @Parameter(description = "告警ID") @PathVariable Long id,
            @Parameter(description = "处理状态：1-已确认 2-已忽略 3-已处理") @RequestParam Integer handleStatus,
            @Parameter(description = "处理人") @RequestParam String handler,
            @Parameter(description = "处理备注") @RequestParam(required = false) String remark) {
        verificationService.handleAlert(id, handleStatus, handler, remark);
        return ApiResponse.success();
    }

    // ==================== 审核验车 ====================

    @Operation(summary = "审核验车记录")
    @PutMapping("/inspection/{id}/review")
    @SaCheckPermission("dms:verification:operate")
    public ApiResponse<Void> reviewInspection(
            @Parameter(description = "验车记录ID") @PathVariable Long id,
            @Parameter(description = "审核结果：1-通过 2-不通过") @RequestParam Integer result,
            @Parameter(description = "审核人") @RequestParam String reviewer,
            @Parameter(description = "审核意见") @RequestParam(required = false) String remark) {
        verificationService.reviewInspection(id, result, reviewer, remark);
        return ApiResponse.success();
    }
}
