package cn.aiedge.dms.orderpool.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.dms.orderpool.dto.BidRequest;
import cn.aiedge.dms.orderpool.dto.CancelBidRequest;
import cn.aiedge.dms.orderpool.dto.GrabRequest;
import cn.aiedge.dms.orderpool.dto.OrderPoolQuery;
import cn.aiedge.dms.orderpool.dto.OrderPoolRowVO;
import cn.aiedge.dms.orderpool.dto.PublishRequest;
import cn.aiedge.dms.orderpool.entity.DmsBid;
import cn.aiedge.dms.orderpool.entity.DmsOrderPool;
import cn.aiedge.dms.orderpool.service.BidService;
import cn.aiedge.dms.orderpool.service.OrderPoolService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 订单池（众包 / 抢单竞价大厅）控制器
 *
 * <p>页面：配送 → 调度管理 → 订单池（菜单 80860）。金标准口径见《订单池开发文档》。</p>
 */
@Slf4j
@Tag(name = "订单池管理")
@RestController
@RequestMapping("/api/dms/order-pool")
@RequiredArgsConstructor
@SaCheckLogin
public class OrderPoolController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OrderPoolService orderPoolService;
    private final BidService bidService;

    // ═══════════════════════════════════════════════
    // 台账查询 / 导出
    // ═══════════════════════════════════════════════

    @Operation(summary = "订单池台账分页（多条件，联查任务主数据）")
    @SaCheckPermission("dms:order-pool:list")
    @GetMapping("/page")
    public ApiResponse<Page<OrderPoolRowVO>> page(
            OrderPoolQuery query,
            @Parameter(description = "当前页") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(orderPoolService.pageQuery(query, (int) current, (int) size));
    }

    @Operation(summary = "导出订单池台账（真实 Excel）")
    @SaCheckPermission("dms:order-pool:export")
    @GetMapping("/export")
    public void export(OrderPoolQuery query, HttpServletResponse response) {
        List<OrderPoolRowVO> list = orderPoolService.pageQuery(query, 1, 10000).getRecords();
        String fileName = "订单池_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        String[] headers = {"任务编号", "关联订单号", "订单类型", "客户", "取货地址", "收货地址", "配送区域",
                "预计里程(km)", "货品金额", "配送费", "竞价模式", "起拍价", "当前价/中标价", "竞价数",
                "池状态", "接单配送员", "发布时间", "过期时间", "下架原因", "备注"};
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("订单池");
            CellStyle headStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headStyle.setFont(bold);
            Row head = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
                sheet.setColumnWidth(i, 18 * 256);
            }
            int rowIdx = 1;
            for (OrderPoolRowVO v : list) {
                Row row = sheet.createRow(rowIdx++);
                int c = 0;
                row.createCell(c++).setCellValue(str(v.getTaskNo()));
                row.createCell(c++).setCellValue(str(v.getOrderNo()));
                row.createCell(c++).setCellValue(str(v.getOrderTypeText()));
                row.createCell(c++).setCellValue(str(v.getCustomerName()));
                row.createCell(c++).setCellValue(str(v.getSourceAddress()));
                row.createCell(c++).setCellValue(str(v.getCustomerAddress()));
                row.createCell(c++).setCellValue(str(v.getRouteArea()));
                setNum(row, c++, v.getEstimatedDistance());
                setNum(row, c++, v.getGoodsAmount());
                setNum(row, c++, v.getDeliveryFee());
                row.createCell(c++).setCellValue(v.getBidEnabled() != null && v.getBidEnabled() == 1 ? "开启" : "关闭");
                setNum(row, c++, v.getBidStartPrice());
                setNum(row, c++, v.getBidCurrentPrice());
                setInt(row, c++, v.getBidCount());
                row.createCell(c++).setCellValue(str(v.getPoolStatusText()));
                row.createCell(c++).setCellValue(str(v.getRiderName()));
                row.createCell(c++).setCellValue(str(v.getPublishedTime()));
                row.createCell(c++).setCellValue(str(v.getExpireTime()));
                row.createCell(c++).setCellValue(str(v.getOfflineReason()));
                row.createCell(c).setCellValue(str(v.getRemark()));
            }
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("导出订单池失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    @Operation(summary = "获取订单池条目详情")
    @SaCheckPermission("dms:order-pool:detail")
    @GetMapping("/{id}")
    public ApiResponse<DmsOrderPool> getById(@Parameter(description = "订单池ID") @PathVariable Long id) {
        return ApiResponse.ok(orderPoolService.getById(id));
    }

    // ═══════════════════════════════════════════════
    // 池生命周期（发布 / 下架 / 竞价开关 / 结算 / 过期）
    // ═══════════════════════════════════════════════

    @Operation(summary = "发布任务到订单池（批量；可同时开启竞价）")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/publish")
    public ApiResponse<List<Long>> publish(@Valid @RequestBody PublishRequest request) {
        List<Long> poolIds = new ArrayList<>();
        for (Long taskId : request.getTaskIds()) {
            DmsOrderPool pool = orderPoolService.publishToPool(taskId, request.getDeliveryFee());
            poolIds.add(pool.getId());
            if (pool.getId() != null && request.getBidStartPrice() != null
                    && request.getDurationMinutes() != null && request.getDurationMinutes() > 0) {
                orderPoolService.enableBid(pool.getId(), request.getBidStartPrice(), request.getDurationMinutes());
            }
        }
        return ApiResponse.ok(poolIds);
    }

    @Operation(summary = "下架订单池条目")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/{id}/offline")
    public ApiResponse<Void> offline(@Parameter(description = "订单池ID") @PathVariable Long id,
                                     @Parameter(description = "下架原因") @RequestParam(required = false) String reason) {
        orderPoolService.offline(id, reason);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "开启竞价（待抢单 → 竞价中）")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/{id}/enable-bid")
    public ApiResponse<Void> enableBid(@Parameter(description = "订单池ID") @PathVariable Long id,
                                       @Parameter(description = "起拍价") @RequestParam BigDecimal startPrice,
                                       @Parameter(description = "竞价时长(分钟)") @RequestParam Integer durationMinutes) {
        orderPoolService.enableBid(id, startPrice, durationMinutes);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "关闭竞价（竞价中 → 待抢单；作废本次全部报价）")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/{id}/disable-bid")
    public ApiResponse<Integer> disableBid(@Parameter(description = "订单池ID") @PathVariable Long id) {
        return ApiResponse.ok(orderPoolService.disableBid(id));
    }

    @Operation(summary = "结算竞价（价低优先）：落中标 + 同事务指派任务")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/{id}/settle")
    public ApiResponse<Long> settle(@Parameter(description = "订单池ID") @PathVariable Long id) {
        Long taskId = orderPoolService.getById(id).getTaskId();
        bidService.settleBid(id);
        return ApiResponse.ok(taskId);
    }

    @Operation(summary = "强制分配（定向指派：池 → 已接单）")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/{id}/force-assign")
    public ApiResponse<Void> forceAssign(@Parameter(description = "订单池ID") @PathVariable Long id,
                                        @Parameter(description = "指派请求") @Valid @RequestBody GrabRequest request) {
        orderPoolService.forceAssign(id, request.getRiderId(), request.getRiderName());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "过期扫描（竞价截止的池 → 已过期，可人工回退到《调度任务》指派）")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/expire-scan")
    public ApiResponse<Integer> expireScan() {
        return ApiResponse.ok(orderPoolService.expirePools());
    }

    // ═══════════════════════════════════════════════
    // 抢单 / 竞价（配送员侧）
    // ═══════════════════════════════════════════════

    @Operation(summary = "抢单（先到先得；校验配送员在线与实名资质）")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/{id}/grab")
    public ApiResponse<Void> grab(@Parameter(description = "订单池ID") @PathVariable Long id,
                                  @Parameter(description = "抢单请求") @Valid @RequestBody GrabRequest request) {
        orderPoolService.grab(id, request.getRiderId(), request.getRiderName());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "出价（竞价中，价低者优）")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/{id}/bid")
    public ApiResponse<DmsBid> bid(@Parameter(description = "订单池ID") @PathVariable Long id,
                                   @Parameter(description = "竞价请求") @Valid @RequestBody BidRequest request) {
        return ApiResponse.ok(bidService.createBid(id, request.getRiderId(), request.getRiderName(), request.getPrice()));
    }

    @Operation(summary = "竞价记录列表（价低优先）")
    @SaCheckPermission("dms:order-pool:view")
    @GetMapping("/{id}/bid-list")
    public ApiResponse<List<DmsBid>> getBidList(@Parameter(description = "订单池ID") @PathVariable Long id) {
        orderPoolService.getById(id);
        return ApiResponse.ok(bidService.getBidsByPoolId(id));
    }

    @Operation(summary = "取消出价")
    @SaCheckPermission("dms:order-pool:update")
    @PostMapping("/{id}/cancel-bid")
    public ApiResponse<Void> cancelBid(@Parameter(description = "订单池ID") @PathVariable Long id,
                                       @Parameter(description = "取消竞价请求") @Valid @RequestBody CancelBidRequest request) {
        bidService.cancelBid(request.getBidId(), request.getRiderId());
        return ApiResponse.ok(null);
    }

    // ═══════════════════════════════════════════════
    // 内部工具
    // ═══════════════════════════════════════════════

    private static void setNum(Row row, int index, BigDecimal value) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : String.valueOf(value.doubleValue()));
    }

    private static void setInt(Row row, int index, Integer value) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : String.valueOf(value));
    }

    private static String str(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dt) {
            return dt.format(DT_FMT);
        }
        return String.valueOf(value);
    }
}
