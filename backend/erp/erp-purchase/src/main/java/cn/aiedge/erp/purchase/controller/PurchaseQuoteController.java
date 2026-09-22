package cn.aiedge.erp.purchase.controller;

import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.entity.PurchaseQuoteItem;
import cn.aiedge.erp.purchase.service.PurchaseQuoteService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 报价Controller - RESTful API
 */
@RestController
@RequestMapping("/api/erp/purchase/quote")
@RequiredArgsConstructor
public class PurchaseQuoteController {

    private final PurchaseQuoteService quoteService;
    private final ObjectMapper objectMapper;

    /**
     * 提交报价
     */
    @SaCheckPermission("purchase:quote:create")
    @PostMapping
    public ResponseEntity<PurchaseSupplierQuote> submitQuote(
            @RequestBody Map<String, Object> request) {
        PurchaseSupplierQuote quote = objectMapper.convertValue(request.get("quote"), PurchaseSupplierQuote.class);
        List<PurchaseQuoteItem> items = objectMapper.convertValue(request.get("items"),
                new TypeReference<List<PurchaseQuoteItem>>() {});
        PurchaseSupplierQuote submitted = quoteService.submitQuote(quote, items);
        return ResponseEntity.ok(submitted);
    }

    /**
     * 更新报价
     */
    @SaCheckPermission("purchase:quote:update")
    @PutMapping("/{id}")
    public ResponseEntity<PurchaseSupplierQuote> updateQuote(
            @PathVariable Long id,
            @RequestBody PurchaseSupplierQuote quote) {
        PurchaseSupplierQuote updated = quoteService.updateQuote(id, quote);
        return ResponseEntity.ok(updated);
    }

    /**
     * 审查报价（评分）
     */
    @SaCheckPermission("purchase:quote:approve")
    @PostMapping("/{id}/review")
    public ResponseEntity<PurchaseSupplierQuote> reviewQuote(
            @PathVariable Long id,
            @RequestBody Map<String, Object> scores) {
        Double priceScore = (Double) scores.get("priceScore");
        Double qualityScore = (Double) scores.get("qualityScore");
        Double serviceScore = (Double) scores.get("serviceScore");
        String reviewComment = (String) scores.get("reviewComment");
        
        PurchaseSupplierQuote reviewed = quoteService.reviewQuote(id, priceScore, qualityScore, serviceScore, reviewComment);
        return ResponseEntity.ok(reviewed);
    }

    /**
     * 接受报价
     */
    @SaCheckPermission("purchase:quote:update")
    @PostMapping("/{id}/accept")
    public ResponseEntity<PurchaseSupplierQuote> acceptQuote(@PathVariable Long id) {
        PurchaseSupplierQuote accepted = quoteService.acceptQuote(id);
        return ResponseEntity.ok(accepted);
    }

    /**
     * 拒绝报价
     */
    @SaCheckPermission("purchase:quote:approve")
    @PostMapping("/{id}/reject")
    public ResponseEntity<PurchaseSupplierQuote> rejectQuote(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.get("reason");
        PurchaseSupplierQuote rejected = quoteService.rejectQuote(id, reason);
        return ResponseEntity.ok(rejected);
    }

    /**
     * 撤回报价
     */
    @SaCheckPermission("purchase:quote:update")
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<PurchaseSupplierQuote> withdrawQuote(@PathVariable Long id) {
        PurchaseSupplierQuote withdrawn = quoteService.withdrawQuote(id);
        return ResponseEntity.ok(withdrawn);
    }

    /**
     * 查询报价详情
     */
    @SaCheckPermission("purchase:quote:detail")
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseSupplierQuote> getQuoteById(@PathVariable Long id) {
        PurchaseSupplierQuote quote = quoteService.getQuoteById(id);
        return ResponseEntity.ok(quote);
    }

    /**
     * 根据编号查询报价
     */
    @SaCheckPermission("purchase:quote:detail")
    @GetMapping("/no/{quoteNo}")
    public ResponseEntity<PurchaseSupplierQuote> getQuoteByNo(@PathVariable String quoteNo) {
        PurchaseSupplierQuote quote = quoteService.getQuoteByNo(quoteNo);
        return ResponseEntity.ok(quote);
    }

    /**
     * 根据询价单查询报价列表
     */
    @SaCheckPermission("purchase:quote:detail")
    @GetMapping("/inquiry/{inquiryId}")
    public ResponseEntity<List<PurchaseSupplierQuote>> getQuotesByInquiry(@PathVariable Long inquiryId) {
        List<PurchaseSupplierQuote> quotes = quoteService.getQuotesByInquiry(inquiryId);
        return ResponseEntity.ok(quotes);
    }

    /**
     * 根据供应商查询报价列表
     */
    @SaCheckPermission("purchase:quote:detail")
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<PurchaseSupplierQuote>> getQuotesBySupplier(@PathVariable Long supplierId) {
        List<PurchaseSupplierQuote> quotes = quoteService.getQuotesBySupplier(supplierId);
        return ResponseEntity.ok(quotes);
    }

    /**
     * 查询询价单的中标报价
     */
    @SaCheckPermission("purchase:quote:view")
    @GetMapping("/inquiry/{inquiryId}/winner")
    public ResponseEntity<PurchaseSupplierQuote> getWinningQuote(@PathVariable Long inquiryId) {
        PurchaseSupplierQuote winner = quoteService.getWinningQuote(inquiryId);
        return ResponseEntity.ok(winner);
    }

    /**
     * 比价分析
     */
    @SaCheckPermission("purchase:quote:view")
    @GetMapping("/inquiry/{inquiryId}/compare")
    public ResponseEntity<String> compareQuotes(@PathVariable Long inquiryId) {
        String analysis = quoteService.compareQuotes(inquiryId);
        return ResponseEntity.ok(analysis);
    }
}