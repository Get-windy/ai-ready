package cn.aiedge.erp.purchase.controller;

import cn.aiedge.erp.purchase.entity.PurchaseInquiry;
import cn.aiedge.erp.purchase.service.PurchaseInquiryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 询价单Controller - RESTful API
 */
@RestController
@RequestMapping("/api/erp/purchase/inquiry")
@RequiredArgsConstructor
public class PurchaseInquiryController {

    private final PurchaseInquiryService inquiryService;

    /**
     * 创建询价单
     */
    @PostMapping
    public ResponseEntity<PurchaseInquiry> createInquiry(@RequestBody PurchaseInquiry inquiry) {
        PurchaseInquiry created = inquiryService.createInquiry(inquiry);
        return ResponseEntity.ok(created);
    }

    /**
     * 更新询价单
     */
    @PutMapping("/{id}")
    public ResponseEntity<PurchaseInquiry> updateInquiry(
            @PathVariable Long id,
            @RequestBody PurchaseInquiry inquiry) {
        PurchaseInquiry updated = inquiryService.updateInquiry(id, inquiry);
        return ResponseEntity.ok(updated);
    }

    /**
     * 发布询价单
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<PurchaseInquiry> publishInquiry(@PathVariable Long id) {
        PurchaseInquiry published = inquiryService.publishInquiry(id);
        return ResponseEntity.ok(published);
    }

    /**
     * 关闭询价单
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<PurchaseInquiry> closeInquiry(@PathVariable Long id) {
        PurchaseInquiry closed = inquiryService.closeInquiry(id);
        return ResponseEntity.ok(closed);
    }

    /**
     * 取消询价单
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseInquiry> cancelInquiry(@PathVariable Long id) {
        PurchaseInquiry cancelled = inquiryService.cancelInquiry(id);
        return ResponseEntity.ok(cancelled);
    }

    /**
     * 分页查询询价单
     */
    @GetMapping("/page")
    public ResponseEntity<Page<PurchaseInquiry>> pageInquiries(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Page<PurchaseInquiry> page = inquiryService.pageInquiries(pageNum, pageSize, keyword, status);
        return ResponseEntity.ok(page);
    }

    /**
     * 查询询价单列表
     */
    @GetMapping
    public ResponseEntity<List<PurchaseInquiry>> getAllInquiries() {
        List<PurchaseInquiry> inquiries = inquiryService.getAllInquiries();
        return ResponseEntity.ok(inquiries);
    }

    /**
     * 根据状态查询询价单
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PurchaseInquiry>> getInquiriesByStatus(@PathVariable String status) {
        List<PurchaseInquiry> inquiries = inquiryService.getInquiriesByStatus(status);
        return ResponseEntity.ok(inquiries);
    }

    /**
     * 根据采购员查询询价单
     */
    @GetMapping("/purchaser/{purchaserId}")
    public ResponseEntity<List<PurchaseInquiry>> getInquiriesByPurchaser(@PathVariable Long purchaserId) {
        List<PurchaseInquiry> inquiries = inquiryService.getInquiriesByPurchaser(purchaserId);
        return ResponseEntity.ok(inquiries);
    }

    /**
     * 查询询价单详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseInquiry> getInquiryById(@PathVariable Long id) {
        PurchaseInquiry inquiry = inquiryService.getInquiryById(id);
        return ResponseEntity.ok(inquiry);
    }

    /**
     * 根据编号查询询价单
     */
    @GetMapping("/no/{inquiryNo}")
    public ResponseEntity<PurchaseInquiry> getInquiryByNo(@PathVariable String inquiryNo) {
        PurchaseInquiry inquiry = inquiryService.getInquiryByNo(inquiryNo);
        return ResponseEntity.ok(inquiry);
    }

    /**
     * 删除询价单
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInquiry(@PathVariable Long id) {
        inquiryService.deleteInquiry(id);
        return ResponseEntity.ok().build();
    }
}