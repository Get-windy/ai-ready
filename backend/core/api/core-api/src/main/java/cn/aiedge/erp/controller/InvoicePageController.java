package cn.aiedge.erp.controller;

import cn.aiedge.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 发票列表控制器（暂未对接真实数据源，返回空结果）
 * 对应前端 CRM 发票列表页面调用 /api/erp/invoice/page
 */
@Slf4j
@RestController
@RequestMapping("/api/erp/invoice")
@Tag(name = "发票管理（临时）", description = "发票管理接口 - 暂未对接真实数据源")
public class InvoicePageController {

    @GetMapping("/page")
    @Operation(summary = "分页获取发票列表")
    public ResponseEntity<Map<String, Object>> getInvoicesPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Getting invoice page - pageNum={}, pageSize={}", pageNum, pageSize);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", Collections.emptyList());
        result.put("total", 0);
        result.put("current", pageNum);
        result.put("size", pageSize);
        result.put("pages", 0);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有发票列表")
    public ResponseEntity<List<Object>> getAllInvoices() {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取发票统计信息")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCount", 0);
        stats.put("totalAmount", 0);
        stats.put("pendingCount", 0);
        stats.put("issuedCount", 0);
        return ResponseEntity.ok(stats);
    }
}
