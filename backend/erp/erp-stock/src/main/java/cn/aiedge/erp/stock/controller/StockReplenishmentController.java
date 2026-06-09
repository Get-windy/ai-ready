package cn.aiedge.erp.stock.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.stock.entity.StockReplenishment;
import cn.aiedge.erp.stock.service.StockReplenishmentService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/stock/replenishment")
@RequiredArgsConstructor
@Tag(name = "智能补货管理", description = "智能补货建议生成、查询、转订单等操作")
public class StockReplenishmentController {

    private final StockReplenishmentService replenishmentService;

    @GetMapping("/list")
    @Operation(summary = "查询补货建议列表")
    public ResponseEntity<Page<StockReplenishment>> list(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "优先级") @RequestParam(required = false) String priority,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Page<StockReplenishment> page = replenishmentService.pageList(keyword, priority, status, pageNum, pageSize);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/generate")
    @Operation(summary = "生成补货建议")
    public ResponseEntity<List<StockReplenishment>> generate() {
        List<StockReplenishment> suggestions = replenishmentService.generateSuggestions();
        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/{id}/create-order")
    @Operation(summary = "根据补货建议创建采购订单")
    public ResponseEntity<StockReplenishment> createOrder(
            @PathVariable Long id,
            @RequestBody(required = false) CreateOrderRequest request) {
        Long supplierId = request != null ? request.getSupplierId() : null;
        StockReplenishment result = replenishmentService.createOrder(id, supplierId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/ignore")
    @Operation(summary = "忽略补货建议")
    public ResponseEntity<StockReplenishment> ignore(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        StockReplenishment result = replenishmentService.ignoreSuggestion(id, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * 创建订单请求体
     */
    public static class CreateOrderRequest {
        private Long supplierId;
        public Long getSupplierId() { return supplierId; }
        public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    }
}
