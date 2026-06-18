package cn.aiedge.dms.tracking.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.tracking.entity.DmsTracking;
import cn.aiedge.dms.tracking.service.TrackingService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 位置追踪控制器
 *
 * 提供配送员位置上报、轨迹查询、历史轨迹回放等REST接口。
 *
 * @author AI-Ready Team
 */
@Tag(name = "位置追踪")
@RestController
@RequestMapping("/api/dms/tracking")
@RequiredArgsConstructor
@SaCheckLogin
public class TrackingController {

    private final TrackingService trackingService;

    @Operation(summary = "上报位置")
    @PostMapping("/report")
    @SaCheckPermission("dms:tracking:report")
    public ApiResponse<Void> reportLocation(
            @Parameter(description = "配送员ID") @RequestParam Long riderId,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "纬度") @RequestParam BigDecimal lat,
            @Parameter(description = "经度") @RequestParam BigDecimal lng,
            @Parameter(description = "速度(km/h)") @RequestParam(required = false) BigDecimal speed,
            @Parameter(description = "方向角度") @RequestParam(required = false) BigDecimal direction) {
        trackingService.reportLocation(riderId, taskId, lat, lng, speed, direction);
        return ApiResponse.success();
    }

    @Operation(summary = "获取配送员最新位置")
    @GetMapping("/latest/{riderId}")
    public ApiResponse<DmsTracking> getLatestLocation(
            @Parameter(description = "配送员ID") @PathVariable Long riderId) {
        DmsTracking record = trackingService.getLatestLocation(riderId);
        return ApiResponse.success(record);
    }

    @Operation(summary = "获取轨迹（时间范围）")
    @GetMapping("/track")
    public ApiResponse<List<DmsTracking>> getTrack(
            @Parameter(description = "配送员ID") @RequestParam Long riderId,
            @Parameter(description = "起始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<DmsTracking> track = trackingService.getTrack(riderId, startTime, endTime);
        return ApiResponse.success(track);
    }

    @Operation(summary = "获取任务轨迹")
    @GetMapping("/task/{taskId}")
    public ApiResponse<List<DmsTracking>> getTrackByTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        List<DmsTracking> track = trackingService.getTrackByTask(taskId);
        return ApiResponse.success(track);
    }
}
