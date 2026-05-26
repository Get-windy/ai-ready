package cn.aiedge.erp.printing.controller;

import cn.aiedge.erp.printing.dto.*;
import cn.aiedge.erp.printing.service.PrintLogService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "打印日志管理", description = "打印日志查询、统计、导出等操作")
@RestController
@RequestMapping("/api/v1/print/logs")
@RequiredArgsConstructor
public class PrintLogController {

    private final PrintLogService logService;

    @Operation(summary = "打印记录查询")
    @GetMapping
    public ResponseEntity<Map<String, Object>> queryLogs(PrintLogQueryRequest request) {
        if (request.getPage() == null) request.setPage(1);
        if (request.getSize() == null) request.setSize(20);
        Page<PrintLogDTO> pageResult = logService.queryLogs(request);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "打印统计")
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "printer") String groupBy) {
        PrintStatisticsDTO stats = logService.getStatistics(startDate, endDate, groupBy);
        return ResponseEntity.ok(success(stats));
    }

    @Operation(summary = "打印日志导出")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "excel") String format) {
        byte[] data = logService.exportLogs(startDate, endDate, format);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "print_logs.xlsx");
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", "200");
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}