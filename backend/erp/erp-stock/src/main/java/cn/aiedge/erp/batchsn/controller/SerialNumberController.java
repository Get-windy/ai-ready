package cn.aiedge.erp.batchsn.controller;

import cn.aiedge.erp.batchsn.entity.SerialNumber;
import cn.aiedge.erp.batchsn.service.SerialNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 序列号管理控制器
 * 
 * @author team-member
 * @date 2026-04-27
 */
@RestController
@RequestMapping("/api/erp/batch-sn/serials")
@RequiredArgsConstructor
public class SerialNumberController {
    
    private final SerialNumberService serialNumberService;
    
    /**
     * 创建序列号
     */
    @PostMapping
    public ResponseEntity<SerialNumber> createSerial(@RequestBody SerialNumber serial) {
        SerialNumber created = serialNumberService.createSerial(serial);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * 查询序列号详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<SerialNumber> getSerial(@PathVariable Long id) {
        SerialNumber serial = serialNumberService.getSerialById(id);
        return ResponseEntity.ok(serial);
    }
    
    /**
     * 查询序列号列表
     */
    @GetMapping
    public ResponseEntity<List<SerialNumber>> listSerials(
        @RequestParam(required = false) String serialNo,
        @RequestParam(required = false) String productCode,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String batchNo,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        List<SerialNumber> serials = serialNumberService.listSerials(serialNo, productCode, status, batchNo, page, size);
        return ResponseEntity.ok(serials);
    }
    
    /**
     * 更新序列号信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<SerialNumber> updateSerial(@PathVariable Long id, @RequestBody SerialNumber serial) {
        SerialNumber updated = serialNumberService.updateSerial(id, serial);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * 序列号入库
     */
    @PostMapping("/inbound")
    public ResponseEntity<SerialNumber> inbound(
        @RequestBody SerialNumber serial,
        @RequestParam Long warehouseId,
        @RequestParam String warehouseName,
        @RequestParam Long locationId
    ) {
        SerialNumber result = serialNumberService.inbound(serial, warehouseId, warehouseName, locationId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 序列号出库
     */
    @PostMapping("/outbound")
    public ResponseEntity<SerialNumber> outbound(
        @RequestParam Long serialId,
        @RequestParam Long saleOrderId,
        @RequestParam String saleOrderNo,
        @RequestParam Long warehouseId,
        @RequestParam Long locationId
    ) {
        SerialNumber result = serialNumberService.outbound(serialId, saleOrderId, saleOrderNo, warehouseId, locationId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 更新序列号状态
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SerialNumber> updateStatus(
        @PathVariable Long id,
        @RequestParam String status,
        @RequestParam String stage
    ) {
        SerialNumber result = serialNumberService.updateStatus(id, status, stage);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 查询质保即将到期的序列号
     */
    @GetMapping("/warranty-warning")
    public ResponseEntity<List<SerialNumber>> getWarrantyExpiring(
        @RequestParam(defaultValue = "30") int warningDays
    ) {
        List<SerialNumber> serials = serialNumberService.getWarrantyExpiring(warningDays);
        return ResponseEntity.ok(serials);
    }
    
    /**
     * 查询序列号完整流转历史
     */
    @GetMapping("/{id}/full-history")
    public ResponseEntity<List<SerialNumber>> getFullHistory(@PathVariable Long id) {
        List<SerialNumber> history = serialNumberService.getFullHistory(id);
        return ResponseEntity.ok(history);
    }
    
    /**
     * 验证序列号唯一性
     */
    @GetMapping("/validate-serial-no")
    public ResponseEntity<Boolean> validateSerialNo(@RequestParam String serialNo) {
        boolean exists = serialNumberService.serialNoExists(serialNo);
        return ResponseEntity.ok(!exists);
    }
}
