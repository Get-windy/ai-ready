package cn.aiedge.erp.delivery.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.delivery.dto.*;
import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import cn.aiedge.erp.delivery.service.RouteCollectionService;
import cn.aiedge.erp.delivery.service.RoutePlanningService;
import cn.aiedge.erp.delivery.service.RouteService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 配送路线单（执行单）Controller
 *
 * 页面：配送 → 配送路线 → 配送路线单（菜单 80700 / dms:route-list）
 * 职责：多点配送执行单台账 —— 建单 / 查询 / 状态流转 / 多点签收 / 导出。
 *
 * 红线：本控制器管理的是**执行单**（谁跑、跑到哪、开始/完成/取消），
 *      线路档案主数据见 {@code RouteMasterController}（/api/erp/md/route，erp_route），二者严格区分、不重复建表。
 *      ql361 无「配送」模块 → 本页为本系统自主建模，不臆造对标字段。
 */
@Tag(name = "配送路线单", description = "多点配送执行单：台账查询、建单、状态流转、多点签收、导出")
@Slf4j
@RestController
@RequestMapping("/api/delivery/route")
@RequiredArgsConstructor
public class RouteController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RouteService routeService;
    private final RouteCollectionService routeCollectionService;
    private final RoutePlanningService routePlanningService;

    // ==================== 台账 ====================

    @Operation(summary = "分页查询配送路线单")
    @GetMapping("/page")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "分页查询配送路线单")
    public ApiResponse<Page<DeliveryRouteVO>> page(DeliveryRouteQueryDTO query) {
        return ApiResponse.success(routeService.pageRoutes(query));
    }

    @Operation(summary = "查询配送路线单列表（不分页，导出复用）")
    @GetMapping("/list")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "查询配送路线单列表")
    public ApiResponse<List<DeliveryRouteVO>> list(DeliveryRouteQueryDTO query) {
        return ApiResponse.success(routeService.listRoutes(query));
    }

    @Operation(summary = "生成下一个路线编号（PSXL-YYYYMMDD-序号）")
    @GetMapping("/next-no")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "生成下一个路线编号")
    public ApiResponse<String> nextNo() {
        return ApiResponse.success(routeService.nextNo());
    }

    @Operation(summary = "路线详情（含点位明细）")
    @GetMapping("/{routeId}")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "查询配送路线单详情")
    public ApiResponse<DeliveryRouteVO> detail(@PathVariable Long routeId) {
        return ApiResponse.success(routeService.getRouteVO(routeId));
    }

    // ==================== 建单 / 改单 ====================

    @Operation(summary = "新增配送路线单（手工建单，不依赖高德 key）")
    @PostMapping
    @OperationLog(module = "配送路线单", type = "CREATE", desc = "新增配送路线单")
    public ApiResponse<DeliveryRouteVO> create(@RequestBody DeliveryRouteSaveDTO dto) {
        return ApiResponse.success("新增成功", routeService.createRoute(dto));
    }

    @Operation(summary = "修改配送路线单（仅规划中/待出发）")
    @PutMapping("/{routeId}")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "修改配送路线单")
    public ApiResponse<DeliveryRouteVO> update(@PathVariable Long routeId, @RequestBody DeliveryRouteSaveDTO dto) {
        return ApiResponse.success("修改成功", routeService.updateRoute(routeId, dto));
    }

    // ==================== 状态流转 ====================

    @Operation(summary = "开始配送（规划中/待出发 → 配送中）")
    @PostMapping("/{routeId}/start")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "开始配送")
    public ApiResponse<Void> start(@PathVariable Long routeId) {
        routeService.startRoute(routeId);
        return ApiResponse.success("已开始配送", null);
    }

    @Operation(summary = "完成配送（配送中 → 已完成，未处理点位收口为已跳过）")
    @PostMapping("/{routeId}/complete")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "完成配送")
    public ApiResponse<Void> complete(@PathVariable Long routeId,
                                      @RequestBody(required = false) Map<String, String> body) {
        routeService.completeRoute(routeId, body == null ? null : body.get("reason"));
        return ApiResponse.success("已完成配送", null);
    }

    @Operation(summary = "取消路线（非终态 → 已取消）")
    @PostMapping("/{routeId}/cancel")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "取消配送路线")
    public ApiResponse<Void> cancel(@PathVariable Long routeId,
                                    @RequestBody(required = false) Map<String, String> body) {
        routeService.cancelRoute(routeId, body == null ? null : body.get("reason"));
        return ApiResponse.success("已取消", null);
    }

    @Operation(summary = "批量状态流转（start / complete / cancel）")
    @PostMapping("/batch-status")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "批量流转配送路线状态")
    public ApiResponse<Map<String, Object>> batchStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = body == null ? null : (List<Object>) body.get("ids");
        List<Long> ids = rawIds == null ? List.of()
                : rawIds.stream().map(v -> Long.valueOf(String.valueOf(v))).toList();
        String action = body == null || body.get("action") == null ? null : String.valueOf(body.get("action"));
        String reason = body == null || body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        return ApiResponse.success("操作完成", routeService.batchStatus(ids, action, reason));
    }

    // ==================== 多点签收 ====================

    @Operation(summary = "点位签收（逐点独立：在途/已到达/已送达/配送失败）")
    @PostMapping("/{routeId}/point/{pointId}/sign")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "配送点位签收")
    public ApiResponse<Void> signPoint(@PathVariable Long routeId,
                                       @PathVariable Long pointId,
                                       @RequestBody RoutePointSignDTO dto) {
        routeService.signPoint(routeId, pointId, dto);
        return ApiResponse.success("操作成功", null);
    }

    // ==================== 配送需求归集（围栏自动 + 手动添加） ====================

    @Operation(summary = "查询可入线的配送需求（销售出库单/销售订单）")
    @GetMapping("/demands")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "查询配送需求")
    public ApiResponse<List<DeliveryDemandVO>> demands(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(routeCollectionService.listDemands(source, keyword, limit));
    }

    @Operation(summary = "围栏自动归集预览（干跑，不落库）")
    @PostMapping("/auto-collect/preview")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "围栏归集预览")
    public ApiResponse<AutoCollectResultVO> autoCollectPreview(@RequestBody(required = false) AutoCollectQueryDTO query) {
        AutoCollectQueryDTO q = query == null ? new AutoCollectQueryDTO() : query;
        q.setDryRun(true);
        return ApiResponse.success(routeCollectionService.autoCollect(q));
    }

    @Operation(summary = "围栏自动归集（命中围栏的配送需求自动入线）")
    @PostMapping("/auto-collect")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "围栏自动归集")
    public ApiResponse<AutoCollectResultVO> autoCollect(@RequestBody(required = false) AutoCollectQueryDTO query) {
        AutoCollectQueryDTO q = query == null ? new AutoCollectQueryDTO() : query;
        q.setDryRun(false);
        return ApiResponse.success("归集完成", routeCollectionService.autoCollect(q));
    }

    @Operation(summary = "客户配送坐标清单（归集弹窗展示缺坐标客户）")
    @GetMapping("/customer-geo")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "查询客户配送坐标")
    public ApiResponse<List<Map<String, Object>>> customerGeo(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(routeCollectionService.listCustomerGeos(limit));
    }

    @Operation(summary = "补录客户配送坐标（围栏归集/地图规划的数据基础）")
    @PutMapping("/customer-geo")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "补录客户配送坐标")
    public ApiResponse<Map<String, Object>> saveCustomerGeo(@RequestBody Map<String, Object> body) {
        Long customerId = body == null || body.get("customerId") == null
                ? null : Long.valueOf(String.valueOf(body.get("customerId")));
        java.math.BigDecimal lat = toDecimal(body == null ? null : body.get("latitude"));
        java.math.BigDecimal lng = toDecimal(body == null ? null : body.get("longitude"));
        return ApiResponse.success("已保存", routeCollectionService.saveCustomerGeo(customerId, lat, lng));
    }

    private java.math.BigDecimal toDecimal(Object v) {
        if (v == null || String.valueOf(v).isBlank()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            throw cn.aiedge.common.exception.BusinessException.badRequest("坐标格式不正确：" + v);
        }
    }

    @Operation(summary = "手动添加配送点位（不受围栏限制）")
    @PostMapping("/{routeId}/add-points")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "手动添加配送点位")
    public ApiResponse<Map<String, Object>> addPoints(@PathVariable Long routeId,
                                                      @RequestBody AddRoutePointsDTO dto) {
        return ApiResponse.success("添加完成", routeCollectionService.addPoints(routeId, dto));
    }

    // ==================== 路线规划 / 催单 / ETA ====================

    @Operation(summary = "按地图能力规划路线顺序（回填顺序/里程/时长，未配 Key 自动降级）")
    @PostMapping("/{routeId}/plan-order")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "配送路线规划")
    public ApiResponse<Map<String, Object>> planOrder(@PathVariable Long routeId) {
        return ApiResponse.success("规划完成", routePlanningService.planOrder(routeId));
    }

    @Operation(summary = "催单：把指定点位移到目标序号，动态调整后续顺序")
    @PostMapping("/{routeId}/point/{pointId}/expedite")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "催单调整配送顺序")
    public ApiResponse<Map<String, Object>> expeditePoint(@PathVariable Long routeId,
                                                          @PathVariable Long pointId,
                                                          @RequestBody(required = false) ExpeditePointDTO dto) {
        return ApiResponse.success("已调整", routePlanningService.expeditePoint(routeId, pointId, dto));
    }

    @Operation(summary = "ETA 预估（各剩余点位预计到达时间）")
    @GetMapping("/{routeId}/eta")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "配送 ETA 预估")
    public ApiResponse<RouteEtaVO> eta(@PathVariable Long routeId) {
        return ApiResponse.success(routePlanningService.calcEta(routeId));
    }

    @Operation(summary = "生成 ETA 客户通知（落库为待发送；短信/推送通道未接入）")
    @PostMapping("/{routeId}/notify-eta")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "生成 ETA 客户通知")
    public ApiResponse<Map<String, Object>> notifyEta(@PathVariable Long routeId,
                                                      @RequestBody(required = false) Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = body == null ? null : (List<Object>) body.get("pointIds");
        List<Long> pointIds = rawIds == null ? List.of()
                : rawIds.stream().map(v -> Long.valueOf(String.valueOf(v))).toList();
        String channel = body == null || body.get("channel") == null ? null : String.valueOf(body.get("channel"));
        Map<String, Object> result = routePlanningService.notifyEta(routeId, pointIds, channel);
        // 事务外投递到消息底座（短信通道），失败不影响已生成的台账
        try {
            result.put("dispatch", routePlanningService.dispatchPending(routeId));
        } catch (Exception e) {
            log.warn("ETA 通知投递消息底座失败: routeId={}", routeId, e);
            result.put("dispatch", Map.of("dispatched", 0, "message", "投递消息底座失败：" + e.getMessage()));
        }
        return ApiResponse.success("已生成", result);
    }

    @Operation(summary = "ETA 通知台账分页")
    @GetMapping("/eta-notify/page")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "查询 ETA 通知台账")
    public ApiResponse<Page<cn.aiedge.erp.delivery.entity.DeliveryEtaNotify>> etaNotifyPage(DeliveryEtaNotifyQueryDTO query) {
        return ApiResponse.success(routePlanningService.pageNotify(query));
    }

    @Operation(summary = "ETA 通知状态回写（标记已发送 / 失败 / 作废）")
    @PostMapping("/eta-notify/status")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "回写 ETA 通知状态")
    public ApiResponse<Integer> etaNotifyStatus(@RequestBody Map<String, Object> body) {
        List<Long> ids = toIds(body == null ? null : body.get("ids"));
        String status = body == null || body.get("status") == null ? null : String.valueOf(body.get("status"));
        String errorMsg = body == null || body.get("errorMsg") == null ? null : String.valueOf(body.get("errorMsg"));
        return ApiResponse.success("已更新", routePlanningService.updateNotifyStatus(ids, status, errorMsg));
    }

    @Operation(summary = "ETA 通知补投递到消息底座（幂等）")
    @PostMapping("/eta-notify/dispatch")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "补投递 ETA 通知")
    public ApiResponse<Map<String, Object>> etaNotifyDispatch(@RequestBody(required = false) Map<String, Object> body) {
        Long routeId = body == null || body.get("routeId") == null
                ? null : Long.valueOf(String.valueOf(body.get("routeId")));
        return ApiResponse.success("已处理", routePlanningService.dispatchPending(routeId));
    }

    @Operation(summary = "ETA 通知发送（通道未接入时明确返回未配置）")
    @PostMapping("/eta-notify/send")
    @OperationLog(module = "配送路线单", type = "UPDATE", desc = "发送 ETA 通知")
    public ApiResponse<Map<String, Object>> etaNotifySend(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(routePlanningService.sendNotify(toIds(body == null ? null : body.get("ids"))));
    }

    private List<Long> toIds(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(v -> Long.valueOf(String.valueOf(v))).toList();
    }

    @Operation(summary = "获取配送员当前进行中的路线")
    @GetMapping("/active/{deliveryPersonId}")
    public ApiResponse<DeliveryRoute> getActiveRoute(@PathVariable String deliveryPersonId) {
        return ApiResponse.success(routeService.getActiveRouteByPerson(deliveryPersonId));
    }

    // ==================== 导出 ====================

    @Operation(summary = "导出配送路线单（真实 xlsx）")
    @GetMapping("/export")
    @OperationLog(module = "配送路线单", type = "QUERY", desc = "导出配送路线单")
    public void export(DeliveryRouteQueryDTO query, HttpServletResponse response) throws IOException {
        List<DeliveryRouteVO> rows = routeService.listRoutes(query);

        String fileName = "配送路线单_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));

        String[] headers = {
                "路线编号", "线路名称", "线路类型", "配送员", "车牌号", "计划配送日期",
                "总点位", "已送达", "配送失败", "完成进度", "起点", "终点",
                "总里程(km)", "预计时长(分钟)", "实际时长(分钟)", "状态",
                "开始时间", "完成时间", "取消时间", "取消原因", "备注", "创建人", "创建时间"
        };

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("配送路线单");
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
            for (DeliveryRouteVO vo : rows) {
                Row row = sheet.createRow(rowIdx++);
                String[] values = {
                        nullSafe(vo.getRouteCode()), nullSafe(vo.getRouteName()), nullSafe(vo.getRouteTypeText()),
                        nullSafe(vo.getDeliveryPersonName()), nullSafe(vo.getVehicleNo()),
                        vo.getPlanDate() == null ? "" : vo.getPlanDate().toString(),
                        num(vo.getTotalPoints()), num(vo.getCompletedPoints()), num(vo.getFailedPoints()),
                        nullSafe(vo.getProgress()), nullSafe(vo.getStartPoint()), nullSafe(vo.getEndPoint()),
                        num(vo.getTotalDistance()), num(vo.getTotalDuration()), num(vo.getActualDuration()),
                        nullSafe(vo.getStatusText()),
                        time(vo.getStartTime()), time(vo.getCompleteTime()), time(vo.getCancelTime()),
                        nullSafe(vo.getCancelReason()), nullSafe(vo.getRemark()),
                        nullSafe(vo.getCreateByName()), time(vo.getCreateTime())
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

    private String num(Number value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String time(LocalDateTime value) {
        return value == null ? "" : value.format(TIME_FMT);
    }
}
