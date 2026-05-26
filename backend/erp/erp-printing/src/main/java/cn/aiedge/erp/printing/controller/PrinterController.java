package cn.aiedge.erp.printing.controller;

import cn.aiedge.erp.printing.dto.PrinterStatusDTO;
import cn.aiedge.erp.printing.entity.Printer;
import cn.aiedge.erp.printing.entity.PrinterGroup;
import cn.aiedge.erp.printing.service.PrinterService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "打印机管理", description = "打印机的注册、状态管理、分组管理等操作")
@RestController
@RequestMapping("/api/v1/print/printers")
@RequiredArgsConstructor
public class PrinterController {

    private final PrinterService printerService;

    @Operation(summary = "注册打印机")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPrinter(@RequestBody Printer printer) {
        Printer created = printerService.createPrinter(printer);
        return ResponseEntity.ok(success(created));
    }

    @Operation(summary = "更新打印机")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updatePrinter(@PathVariable Long id, @RequestBody Printer printer) {
        Printer updated = printerService.updatePrinter(id, printer);
        return ResponseEntity.ok(success(updated));
    }

    @Operation(summary = "获取打印机详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPrinter(@PathVariable Long id) {
        Printer printer = printerService.getPrinterById(id);
        return ResponseEntity.ok(success(printer));
    }

    @Operation(summary = "打印机列表查询")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listPrinters(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String printerType,
            @RequestParam(required = false) String status) {
        Page<Printer> pageResult = printerService.listPrinters(page, size, printerType, status);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "删除打印机")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deletePrinter(@PathVariable Long id) {
        printerService.deletePrinter(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "获取打印机状态")
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getPrinterStatus(@PathVariable Long id) {
        PrinterStatusDTO status = printerService.getPrinterStatus(id);
        return ResponseEntity.ok(success(status));
    }

    @Operation(summary = "更新打印机状态")
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updatePrinterStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam Boolean isOnline) {
        printerService.updatePrinterStatus(id, status, isOnline);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "创建打印机分组")
    @PostMapping("/groups")
    public ResponseEntity<Map<String, Object>> createGroup(@RequestBody PrinterGroup group) {
        PrinterGroup created = printerService.createGroup(group);
        return ResponseEntity.ok(success(created));
    }

    @Operation(summary = "更新打印机分组")
    @PutMapping("/groups/{id}")
    public ResponseEntity<Map<String, Object>> updateGroup(@PathVariable Long id, @RequestBody PrinterGroup group) {
        PrinterGroup updated = printerService.updateGroup(id, group);
        return ResponseEntity.ok(success(updated));
    }

    @Operation(summary = "删除打印机分组")
    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable Long id) {
        printerService.deleteGroup(id);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "打印机分组列表")
    @GetMapping("/groups")
    public ResponseEntity<Map<String, Object>> listGroups(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Page<PrinterGroup> pageResult = printerService.listGroups(page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "分配打印机到分组")
    @PostMapping("/groups/{groupId}/assign")
    public ResponseEntity<Map<String, Object>> assignPrinters(
            @PathVariable Long groupId,
            @RequestBody List<Long> printerIds) {
        printerService.assignPrintersToGroup(groupId, printerIds);
        return ResponseEntity.ok(success(null));
    }

    @Operation(summary = "获取分组下的打印机")
    @GetMapping("/groups/{groupId}/printers")
    public ResponseEntity<Map<String, Object>> listPrintersByGroup(@PathVariable Long groupId) {
        List<Printer> printers = printerService.listPrintersByGroup(groupId);
        return ResponseEntity.ok(success(printers));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", "200");
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}