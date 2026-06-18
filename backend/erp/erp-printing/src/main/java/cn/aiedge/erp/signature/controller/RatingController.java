package cn.aiedge.erp.signature.controller;

import cn.aiedge.erp.signature.dto.*;
import cn.aiedge.erp.signature.entity.DeliveryRating;
import cn.aiedge.erp.signature.service.RatingService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "配送评价管理", description = "配送评价提交、查询、统计")
@RestController
@RequestMapping("/api/rating")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @Operation(summary = "提交配送评价")
    @PostMapping
    public ResponseEntity<Map<String, Object>> createRating(@RequestBody RatingCreateRequest request) {
        DeliveryRating rating = ratingService.createRating(request);
        return ResponseEntity.ok(success(rating));
    }

    @Operation(summary = "获取评价详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRating(@PathVariable Long id) {
        DeliveryRating rating = ratingService.getRatingById(id);
        return ResponseEntity.ok(success(rating));
    }

    @Operation(summary = "获取签收记录的评价")
    @GetMapping("/signature/{signatureId}")
    public ResponseEntity<Map<String, Object>> getRatingBySignature(@PathVariable Long signatureId) {
        DeliveryRating rating = ratingService.getRatingBySignatureId(signatureId);
        return ResponseEntity.ok(success(rating));
    }

    @Operation(summary = "评价列表查询")
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listRatings(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long deliveryPersonId) {
        Page<DeliveryRating> pageResult = ratingService.listRatings(page, size, deliveryPersonId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return ResponseEntity.ok(success(result));
    }

    @Operation(summary = "配送员评价统计")
    @GetMapping("/stats/{deliveryPersonId}")
    public ResponseEntity<Map<String, Object>> getRatingStats(@PathVariable Long deliveryPersonId) {
        Map<String, Object> stats = ratingService.getRatingStats(deliveryPersonId);
        return ResponseEntity.ok(success(stats));
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", data);
        return result;
    }
}