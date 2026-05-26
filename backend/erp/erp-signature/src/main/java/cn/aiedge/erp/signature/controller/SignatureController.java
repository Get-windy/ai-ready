package cn.aiedge.erp.signature.controller;

import cn.aiedge.erp.signature.dto.*;
import cn.aiedge.erp.signature.entity.SignatureRecord;
import cn.aiedge.erp.signature.service.SignatureService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "电子签收管理", description = "拍照签收、电子签名、签收记录查询、防篡改验证")
@RestController
@RequestMapping("/api/signature")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    @Operation(summary = "拍照签收")
    @PostMapping("/photo")
    public ResponseEntity<Map<String, Object>> photoSignature(
            @RequestParam String orderNo,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String signerName,
            @RequestParam(required = false) String signerPhone,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String deliveryPersonId,
            @RequestParam(required = false) String deliveryPersonName,
            @RequestParam(required = false) String remark,
            @RequestParam("photo") MultipartFile photo) {
        PhotoSignatureRequest request = new PhotoSignatureRequest();
        request.setOrderNo(orderNo);
        request.setOrderId(orderId);
        request.setSignerName(signerName);
        request.setSignerPhone(signerPhone);
        request.setLocation(location);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setDeviceId(deviceId);
        request.setDeliveryPersonId(deliveryPersonId);
        request.setDeliveryPersonName(deliveryPersonName);
        request.setRemark(remark);
        SignatureRecord record = signatureService.photoSignature(request, photo);
        return ResponseEntity.ok(success(record));
    }

    @Operation(summary = "电子签名签收")
    @PostMapping("/electronic")
    public ResponseEntity<Map<String, Object>> electronicSignature(@RequestBody ElectronicSignatureRequest request) {
        SignatureRecord record = signatureService.electronicSignature(request);
        return ResponseEntity.ok(success(record));
    }

    @Operation(summary = "获取签收详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSignature(@PathVariable Long id) {
        SignatureRecord record = signatureService.getSignatureById(id);
        return ResponseEntity.ok(success(record));
    }

    @Operation(summary = "签收防篡改验证")
    @GetMapping("/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyIntegrity(@PathVariable Long id) {
        IntegrityVerifyResult result = signatureService.verifyIntegrity(id);
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "签收记录列表查询")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listSignatures(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String signatureType) {
        Page<SignatureRecord> pageResult = signatureService.listSignatures(page, size, orderNo, signatureType);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("page", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "获取订单最新签收记录")
    @GetMapping("/order/{orderNo}")
    public ResponseEntity<Map<String, Object>> getLatestByOrderNo(@PathVariable String orderNo) {
        SignatureRecord record = signatureService.getLatestByOrderNo(orderNo);
        return ResponseEntity.ok(success(record));
    }

    @Operation(summary = "配送员签收记录查询")
    @GetMapping("/delivery-person/{deliveryPersonId}")
    public ResponseEntity<Map<String, Object>> listByDeliveryPerson(
            @PathVariable String deliveryPersonId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<SignatureRecord> pageResult = signatureService.listByDeliveryPerson(deliveryPersonId, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return ResponseEntity.ok(success(result));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}