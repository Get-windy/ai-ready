package cn.aiedge.dms.tracking.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.tracking.dto.RiderLocationQuery;
import cn.aiedge.dms.tracking.dto.RiderLocationVO;
import cn.aiedge.dms.tracking.dto.TrackingAlertVO;
import cn.aiedge.dms.tracking.dto.TrackingMileageVO;
import cn.aiedge.dms.tracking.dto.TrackingStatVO;
import cn.aiedge.dms.tracking.dto.TrackingQueryDTO;
import cn.aiedge.dms.tracking.dto.TrackingVO;
import cn.aiedge.dms.tracking.entity.DmsTracking;
import cn.aiedge.dms.tracking.service.TrackingService;
import cn.aiedge.dms.tracking.service.TrackingStreamService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 位置追踪控制器（配送 → 配送跟踪 → 配送跟踪，菜单 80870 / `dms:tracking`）
 *
 * <p>《配送跟踪开发文档》§3.4 金标准接口：补齐**台账分页**（原 `/page` 未实现 → 页面必 404）、
 * **里程聚合**与**导出**；`report / latest / track / task` 保持兼容并扩展定位精度与地址。</p>
 *
 * @author AI-Ready Team
 */
@Tag(name = "位置追踪", description = "轨迹台账 / 里程聚合 / 位置上报 / 导出")
@RestController
@RequestMapping("/api/dms/tracking")
@RequiredArgsConstructor
@SaCheckLogin
public class TrackingController {

    private final TrackingService trackingService;
    private final TrackingStreamService trackingStreamService;

    // ==================== 台账 ====================

    @Operation(summary = "轨迹台账分页（多条件）")
    @OperationLog(module = "配送跟踪", type = "QUERY", desc = "轨迹台账查询")
    @GetMapping("/page")
    public ApiResponse<Page<TrackingVO>> page(TrackingQueryDTO query) {
        return ApiResponse.ok(trackingService.page(query));
    }

    @Operation(summary = "里程聚合（按配送员 / 任务 / 日）")
    @GetMapping("/mileage")
    public ApiResponse<List<TrackingMileageVO>> mileage(
            TrackingQueryDTO query,
            @Parameter(description = "聚合维度：rider（默认）/ task / day") @RequestParam(required = false) String groupBy) {
        return ApiResponse.ok(trackingService.mileage(query, groupBy));
    }

    @Operation(summary = "导出轨迹台账（真实 xlsx）")
    @OperationLog(module = "配送跟踪", type = "EXPORT", desc = "轨迹台账导出")
    @GetMapping("/export")
    public void export(TrackingQueryDTO query, HttpServletResponse response) throws IOException {
        trackingService.export(query, response);
    }

    // ==================== 实时跟踪（配送员位置聚合 / 统计 / 预警 / 回放） ====================

    @Operation(summary = "配送员实时位置分页（一次聚合，替代 N+1）")
    @GetMapping("/rider-page")
    public ApiResponse<Page<RiderLocationVO>> riderPage(RiderLocationQuery query) {
        return ApiResponse.ok(trackingService.riderPage(query));
    }

    @Operation(summary = "实时跟踪统计卡（后端聚合）")
    @GetMapping("/stat")
    public ApiResponse<TrackingStatVO> stat() {
        return ApiResponse.ok(trackingService.stat());
    }

    @Operation(summary = "异常预警（超速 / 异常停留 / 超时在途）")
    @GetMapping("/alerts")
    public ApiResponse<List<TrackingAlertVO>> alerts(TrackingQueryDTO query) {
        return ApiResponse.ok(trackingService.alerts(query));
    }

    @Operation(summary = "轨迹回放点序列（按时间轴抽稀；riderId 与 taskId 二选一）")
    @OperationLog(module = "配送跟踪", type = "QUERY", desc = "轨迹回放查询")
    @GetMapping("/replay")
    public ApiResponse<List<TrackingVO>> replay(
            @Parameter(description = "配送员ID") @RequestParam(required = false) Long riderId,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "起始时间 yyyy-MM-dd[ HH:mm:ss]") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "抽稀目标点数（默认 500，首尾必留）") @RequestParam(required = false) Integer maxPoints) {
        if (taskId != null) {
            return ApiResponse.ok(trackingService.trackByTaskVO(taskId, maxPoints));
        }
        return ApiResponse.ok(trackingService.trackVO(riderId, startTime, endTime, maxPoints));
    }

    @Operation(summary = "实时位置推送（SSE，在线配送员位置增量；Authorization 标准鉴权）")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return trackingStreamService.subscribe();
    }

    @Operation(summary = "合规：按保留策略清理历史轨迹（返回删除条数；days 缺省读配置）")
    @PostMapping("/clean-expired")
    public ApiResponse<Integer> cleanExpired(
            @Parameter(description = "保留天数；缺省读 dms.tracking.retention.days") @RequestParam(required = false) Integer days) {
        int retention = days != null && days > 0 ? days : trackingService.retentionDays();
        if (retention <= 0) {
            return ApiResponse.ok(0);
        }
        return ApiResponse.ok(trackingService.cleanExpiredData(retention));
    }

    // ==================== 轨迹（含抽稀） ====================

    @Operation(summary = "按配送员取轨迹（供地图绘制/回放，含抽稀）")
    @OperationLog(module = "配送跟踪", type = "QUERY", desc = "轨迹明细查询（按配送员）")
    @GetMapping("/track-vo")
    public ApiResponse<List<TrackingVO>> trackVO(
            @Parameter(description = "配送员ID") @RequestParam Long riderId,
            @Parameter(description = "起始时间 yyyy-MM-dd[ HH:mm:ss]") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
            @Parameter(description = "抽稀目标点数（默认 500，首尾点必留）") @RequestParam(required = false) Integer maxPoints) {
        return ApiResponse.ok(trackingService.trackVO(riderId, startTime, endTime, maxPoints));
    }

    @Operation(summary = "按任务取轨迹（供地图绘制，含抽稀）")
    @OperationLog(module = "配送跟踪", type = "QUERY", desc = "轨迹明细查询（按任务）")
    @GetMapping("/task-vo/{taskId}")
    public ApiResponse<List<TrackingVO>> trackByTaskVO(
            @Parameter(description = "任务ID") @PathVariable Long taskId,
            @Parameter(description = "抽稀目标点数") @RequestParam(required = false) Integer maxPoints) {
        return ApiResponse.ok(trackingService.trackByTaskVO(taskId, maxPoints));
    }

    // ==================== 上报 / 既有查询 ====================

    @Operation(summary = "上报位置")
    @PostMapping("/report")
    @SaCheckPermission("dms:tracking:report")
    public ApiResponse<Void> reportLocation(
            @Parameter(description = "配送员ID") @RequestParam Long riderId,
            @Parameter(description = "任务ID") @RequestParam(required = false) Long taskId,
            @Parameter(description = "纬度") @RequestParam BigDecimal lat,
            @Parameter(description = "经度") @RequestParam BigDecimal lng,
            @Parameter(description = "速度(km/h)") @RequestParam(required = false) BigDecimal speed,
            @Parameter(description = "方向角度") @RequestParam(required = false) BigDecimal direction,
            @Parameter(description = "定位精度(米)") @RequestParam(required = false) BigDecimal accuracy,
            @Parameter(description = "地址（上报端逆编码结果）") @RequestParam(required = false) String address,
            @Parameter(description = "来源：1-APP 2-后台补录 3-渠道回传") @RequestParam(required = false) Integer source) {
        trackingService.reportLocation(riderId, taskId, lat, lng, speed, direction, accuracy, address, source);
        return ApiResponse.success();
    }

    @Operation(summary = "获取配送员最新位置")
    @GetMapping("/latest/{riderId}")
    public ApiResponse<DmsTracking> getLatestLocation(
            @Parameter(description = "配送员ID") @PathVariable Long riderId) {
        return ApiResponse.success(trackingService.getLatestLocation(riderId));
    }

    @Operation(summary = "获取轨迹（时间范围，原始实体，兼容旧接口）")
    @GetMapping("/track")
    public ApiResponse<List<DmsTracking>> getTrack(
            @Parameter(description = "配送员ID") @RequestParam Long riderId,
            @Parameter(description = "起始时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return ApiResponse.success(trackingService.getTrack(riderId, startTime, endTime));
    }

    @Operation(summary = "获取任务轨迹（原始实体，兼容旧接口）")
    @GetMapping("/task/{taskId}")
    public ApiResponse<List<DmsTracking>> getTrackByTask(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        return ApiResponse.success(trackingService.getTrackByTask(taskId));
    }
}
