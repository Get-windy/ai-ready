package cn.aiedge.trade.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.trade.entity.InventorySyncRecord;
import cn.aiedge.trade.mapper.InventorySyncRecordMapper;
import cn.aiedge.trade.monitor.ErrorCategory;
import cn.aiedge.trade.monitor.TimeParsers;
import cn.aiedge.trade.monitor.XlsxExporter;
import cn.aiedge.trade.monitor.service.ApiMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 库存同步记录（配送 → API监控 页「库存同步记录」Tab）
 *
 * <p>真实读取 `inventory_sync_record`（按渠道/SKU/类型/状态/失败分类/时间过滤、按同步时间倒序）；
 * 无数据时返回空列表，不做任何模拟数据。导出为**真实 xlsx**（非 JSON）。</p>
 */
@Tag(name = "库存同步记录")
@RestController
@RequestMapping("/api/trade/inventory-sync")
@RequiredArgsConstructor
public class InventorySyncController {

    private static final String[] EXPORT_HEADERS = {
            "渠道", "SKU编码", "同步数量", "类型", "状态", "失败分类", "错误信息", "重试次数", "最近重试时间", "同步时间"
    };

    private final InventorySyncRecordMapper syncRecordMapper;
    /** 重试逻辑与 /api/trade/api-monitor/sync/{id}/retry 共用同一实现（回写原记录，不追加新记录） */
    private final ApiMonitorService apiMonitorService;

    @Operation(summary = "库存同步记录分页")
    @SaCheckPermission("trade:inventory-sync:list")
    @GetMapping("/page")
    public Result<PageResult<InventorySyncRecord>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String syncType,
            @RequestParam(required = false) Integer syncStatus,
            @RequestParam(required = false) String errorCategory,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        Page<InventorySyncRecord> page = new Page<>(pageNum, pageSize);
        Page<InventorySyncRecord> result = syncRecordMapper.selectPage(page, buildWrapper(
                channelCode, skuCode, syncType, syncStatus, errorCategory, startTime, endTime));
        return Result.success(new PageResult<>(
                result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize()));
    }

    @Operation(summary = "库存同步状态统计",
            description = "真实聚合 SQL（COUNT + GROUP BY，租户过滤）：待同步 / 成功 / 失败 笔数，非当前页口径")
    @SaCheckPermission("trade:inventory-sync:view")
    @GetMapping("/stat")
    public Result<Map<String, Object>> stat() {
        long pending = 0L;
        long success = 0L;
        long failed = 0L;
        for (Map<String, Object> row : syncRecordMapper.countBySyncStatus()) {
            long count = row.get("count") instanceof Number ? ((Number) row.get("count")).longValue() : 0L;
            int status = row.get("syncStatus") instanceof Number ? ((Number) row.get("syncStatus")).intValue() : -1;
            switch (status) {
                case 1 -> success += count;
                case 2 -> failed += count;
                case 0 -> pending += count;
                default -> pending += count; // sync_status 为空的历史行按「待同步」计
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", pending + success + failed);
        out.put("pendingCount", pending);
        out.put("successCount", success);
        out.put("failedCount", failed);
        return Result.success(out);
    }

    @Operation(summary = "同步失败重试",
            description = "按记录ID重放推送渠道并回写原记录（重试次数/结果/错误分类），"
                    + "等价于 /api/trade/api-monitor/sync/{id}/retry，供「库存同步记录」页直接调用")
    @SaCheckPermission("trade:inventory-sync:retry")
    @PostMapping("/{id}/retry")
    public Result<Map<String, Object>> retry(@PathVariable Long id) {
        return Result.success(apiMonitorService.retrySync(id));
    }

    @Operation(summary = "库存同步记录导出（真实 xlsx）")
    @SaCheckPermission("trade:inventory-sync:export")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String syncType,
            @RequestParam(required = false) Integer syncStatus,
            @RequestParam(required = false) String errorCategory,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletResponse response) throws IOException {
        List<InventorySyncRecord> records = syncRecordMapper.selectList(buildWrapper(
                channelCode, skuCode, syncType, syncStatus, errorCategory, startTime, endTime));
        List<String[]> rows = records.stream().map(record -> new String[]{
                nullSafe(record.getChannelCode()), nullSafe(record.getSkuCode()),
                record.getSyncQty() == null ? "" : String.valueOf(record.getSyncQty()),
                nullSafe(record.getSyncType()), statusText(record.getSyncStatus()),
                categoryText(record.getErrorCategory()), nullSafe(record.getErrorMsg()),
                record.getRetryCount() == null ? "0" : String.valueOf(record.getRetryCount()),
                time(record.getLastRetryTime()), time(record.getSyncTime())
        }).toList();
        XlsxExporter.write(response, "库存同步记录_" + LocalDate.now() + ".xlsx", "库存同步记录",
                EXPORT_HEADERS, rows);
    }

    private LambdaQueryWrapper<InventorySyncRecord> buildWrapper(
            String channelCode, String skuCode, String syncType, Integer syncStatus,
            String errorCategory, String startTime, String endTime) {
        LambdaQueryWrapper<InventorySyncRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(channelCode), InventorySyncRecord::getChannelCode, channelCode);
        wrapper.like(StringUtils.hasText(skuCode), InventorySyncRecord::getSkuCode, skuCode);
        wrapper.eq(StringUtils.hasText(syncType), InventorySyncRecord::getSyncType, syncType);
        wrapper.eq(syncStatus != null, InventorySyncRecord::getSyncStatus, syncStatus);
        wrapper.eq(StringUtils.hasText(errorCategory), InventorySyncRecord::getErrorCategory, errorCategory);
        LocalDateTime from = TimeParsers.parse(startTime);
        LocalDateTime to = TimeParsers.parse(endTime);
        wrapper.ge(from != null, InventorySyncRecord::getSyncTime, from);
        wrapper.lt(to != null, InventorySyncRecord::getSyncTime, to);
        wrapper.orderByDesc(InventorySyncRecord::getSyncTime).orderByDesc(InventorySyncRecord::getId);
        return wrapper;
    }

    private static String statusText(Integer syncStatus) {
        if (syncStatus == null) {
            return "";
        }
        return switch (syncStatus) {
            case 1 -> "成功";
            case 2 -> "失败";
            default -> "待同步";
        };
    }

    private static String categoryText(String category) {
        if (!StringUtils.hasText(category)) {
            return "";
        }
        try {
            return ErrorCategory.valueOf(category).getLabel();
        } catch (IllegalArgumentException e) {
            return category;
        }
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String time(LocalDateTime value) {
        return value == null ? "" : value.toString().replace('T', ' ');
    }
}
