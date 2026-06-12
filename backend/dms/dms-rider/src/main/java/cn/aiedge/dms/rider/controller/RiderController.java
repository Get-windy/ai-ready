package cn.aiedge.dms.rider.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.rider.entity.DmsRider;
import cn.aiedge.dms.rider.service.RiderService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 运力管理控制器
 */
@Tag(name = "运力管理")
@RestController
@RequestMapping("/api/dms/rider")
@RequiredArgsConstructor
public class RiderController {

    private final RiderService riderService;

    @Operation(summary = "分页查询配送员")
    @GetMapping("/page")
    @SaCheckLogin
    public ApiResponse<Page<DmsRider>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            DmsRider query) {
        Page<DmsRider> page = new Page<>(pageNum, pageSize);
        return ApiResponse.ok(riderService.page(page, query));
    }

    @Operation(summary = "获取配送员详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public ApiResponse<DmsRider> getDetail(@Parameter(description = "配送员ID") @PathVariable Long id) {
        return ApiResponse.ok(riderService.getById(id));
    }

    @Operation(summary = "新增配送员")
    @PostMapping
    @SaCheckPermission("dms:rider:create")
    public ApiResponse<DmsRider> create(@Parameter(description = "配送员信息") @Valid @RequestBody DmsRider rider) {
        return ApiResponse.ok("创建成功", riderService.create(rider));
    }

    @Operation(summary = "更新配送员")
    @PutMapping("/{id}")
    @SaCheckPermission("dms:rider:update")
    public ApiResponse<DmsRider> update(@Parameter(description = "配送员ID") @PathVariable Long id, @Parameter(description = "配送员信息") @Valid @RequestBody DmsRider rider) {
        return ApiResponse.ok("更新成功", riderService.update(id, rider));
    }

    @Operation(summary = "更新接单状态")
    @PostMapping("/{id}/status")
    @SaCheckLogin
    public ApiResponse<Void> updateStatus(@Parameter(description = "配送员ID") @PathVariable Long id, @Parameter(description = "状态: 0=下线, 1=空闲, 2=忙碌") @RequestParam Integer status) {
        riderService.updateStatus(id, status);
        return ApiResponse.ok("状态更新成功", null);
    }

    @Operation(summary = "审核配送员")
    @PostMapping("/{id}/approve")
    @SaCheckPermission("dms:rider:approve")
    public ApiResponse<Void> approve(@Parameter(description = "配送员ID") @PathVariable Long id, @Parameter(description = "审核状态: 1=通过, 2=拒绝") @RequestParam Integer verifyStatus) {
        riderService.updateVerifyStatus(id, verifyStatus);
        return ApiResponse.ok(verifyStatus == 1 ? "审核通过" : "审核拒绝", null);
    }

    @Operation(summary = "位置上报")
    @PostMapping("/location")
    @SaCheckLogin
    public ApiResponse<Void> reportLocation(@Parameter(description = "位置信息") @RequestBody LocationRequest request) {
        riderService.updateLocation(request.riderId, request.lat, request.lng);
        return ApiResponse.ok("位置上报成功", null);
    }

    @Operation(summary = "删除配送员")
    @DeleteMapping("/{id}")
    @SaCheckPermission("dms:rider:delete")
    public ApiResponse<Void> delete(@Parameter(description = "配送员ID") @PathVariable Long id) {
        riderService.delete(id);
        return ApiResponse.ok("删除成功", null);
    }

    /**
     * 位置上报请求体
     */
    public record LocationRequest(Long riderId, BigDecimal lat, BigDecimal lng) {
    }
}
