package cn.aiedge.erp.invoice.controller;

import cn.aiedge.erp.invoice.model.entity.InvoiceApplication;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.service.InvoiceApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 发票申请控制器
 * 提供发票申请相关的REST API接口
 */
@RestController
@RequestMapping("/api/erp/invoice/application")
public class InvoiceApplicationController {

    @Autowired
    private InvoiceApplicationService invoiceApplicationService;

    /**
     * 创建发票申请
     *
     * @param application 发票申请对象
     * @return 创建的发票申请
     */
    @PostMapping
    public ResponseEntity<InvoiceApplication> createInvoiceApplication(@Valid @RequestBody InvoiceApplication application) {
        InvoiceApplication created = invoiceApplicationService.createInvoiceApplication(application);
        return ResponseEntity.ok(created);
    }

    /**
     * 根据ID获取发票申请详情
     *
     * @param id 申请ID
     * @return 发票申请对象
     */
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceApplication> getInvoiceApplicationById(@PathVariable Long id) {
        Optional<InvoiceApplication> application = invoiceApplicationService.getInvoiceApplicationById(id);
        return application.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有发票申请列表
     *
     * @return 发票申请列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<InvoiceApplication>> getAllInvoiceApplications() {
        List<InvoiceApplication> applications = invoiceApplicationService.getAllInvoiceApplications();
        return ResponseEntity.ok(applications);
    }

    /**
     * 分页获取发票申请列表
     *
     * @param pageable 分页参数
     * @return 发票申请分页
     */
    @GetMapping("/page")
    public ResponseEntity<Page<InvoiceApplication>> getInvoiceApplicationsPage(Pageable pageable) {
        Page<InvoiceApplication> applications = invoiceApplicationService.getInvoiceApplications(pageable);
        return ResponseEntity.ok(applications);
    }

    /**
     * 根据状态获取发票申请列表
     *
     * @param status 申请状态
     * @return 发票申请列表
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvoiceApplication>> getInvoiceApplicationsByStatus(@PathVariable InvoiceStatus status) {
        List<InvoiceApplication> applications = invoiceApplicationService.getInvoiceApplicationsByStatus(status);
        return ResponseEntity.ok(applications);
    }

    /**
     * 根据客户ID获取发票申请列表
     *
     * @param customerId 客户ID
     * @return 发票申请列表
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<InvoiceApplication>> getInvoiceApplicationsByCustomerId(@PathVariable Long customerId) {
        List<InvoiceApplication> applications = invoiceApplicationService.getInvoiceApplicationsByCustomerId(customerId);
        return ResponseEntity.ok(applications);
    }

    /**
     * 根据供应商ID获取发票申请列表
     *
     * @param supplierId 供应商ID
     * @return 发票申请列表
     */
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<InvoiceApplication>> getInvoiceApplicationsBySupplierId(@PathVariable Long supplierId) {
        List<InvoiceApplication> applications = invoiceApplicationService.getInvoiceApplicationsBySupplierId(supplierId);
        return ResponseEntity.ok(applications);
    }

    /**
     * 根据日期范围获取发票申请列表
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 发票申请列表
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<InvoiceApplication>> getInvoiceApplicationsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<InvoiceApplication> applications = invoiceApplicationService.getInvoiceApplicationsByDateRange(startDate, endDate);
        return ResponseEntity.ok(applications);
    }

    /**
     * 更新发票申请
     *
     * @param id 申请ID
     * @param application 更新的申请对象
     * @return 更新后的申请对象
     */
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceApplication> updateInvoiceApplication(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceApplication application) {
        InvoiceApplication updated = invoiceApplicationService.updateInvoiceApplication(id, application);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除发票申请（逻辑删除）
     *
     * @param id 申请ID
     * @param deletedBy 删除人ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteInvoiceApplication(@PathVariable Long id, @RequestParam String deletedBy) {
        boolean result = invoiceApplicationService.deleteInvoiceApplication(id, deletedBy);
        return ResponseEntity.ok(result);
    }

    /**
     * 提交发票申请
     *
     * @param id 申请ID
     * @param submittedBy 提交人ID
     * @return 操作结果
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<Boolean> submitInvoiceApplication(@PathVariable Long id, @RequestParam Long submittedBy) {
        boolean result = invoiceApplicationService.submitInvoiceApplication(id, submittedBy);
        return ResponseEntity.ok(result);
    }

    /**
     * 批准发票申请
     *
     * @param id 申请ID
     * @param approvedBy 批准人ID
     * @param notes 批准意见
     * @return 操作结果
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Boolean> approveInvoiceApplication(
            @PathVariable Long id,
            @RequestParam Long approvedBy,
            @RequestParam(required = false) String notes) {
        boolean result = invoiceApplicationService.approveInvoiceApplication(id, approvedBy, notes);
        return ResponseEntity.ok(result);
    }

    /**
     * 拒绝发票申请
     *
     * @param id 申请ID
     * @param rejectedBy 拒绝人ID
     * @param reason 拒绝原因
     * @return 操作结果
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Boolean> rejectInvoiceApplication(
            @PathVariable Long id,
            @RequestParam Long rejectedBy,
            @RequestParam String reason) {
        boolean result = invoiceApplicationService.rejectInvoiceApplication(id, rejectedBy, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * 取消发票申请
     *
     * @param id 申请ID
     * @param cancelledBy 取消人ID
     * @param reason 取消原因
     * @return 操作结果
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Boolean> cancelInvoiceApplication(
            @PathVariable Long id,
            @RequestParam Long cancelledBy,
            @RequestParam String reason) {
        boolean result = invoiceApplicationService.cancelInvoiceApplication(id, cancelledBy, reason);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成发票
     *
     * @param id 申请ID
     * @param issuedBy 开票人ID
     * @param issuedByName 开票人姓名
     * @return 生成的发票ID
     */
    @PostMapping("/{id}/generate-invoice")
    public ResponseEntity<Long> generateInvoiceFromApplication(
            @PathVariable Long id,
            @RequestParam Long issuedBy,
            @RequestParam String issuedByName) {
        Long invoiceId = invoiceApplicationService.generateInvoiceFromApplication(id, issuedBy, issuedByName);
        return ResponseEntity.ok(invoiceId);
    }

    /**
     * 搜索发票申请
     *
     * @param keyword 关键词
     * @return 发票申请列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<InvoiceApplication>> searchInvoiceApplications(@RequestParam String keyword) {
        List<InvoiceApplication> applications = invoiceApplicationService.searchInvoiceApplications(keyword);
        return ResponseEntity.ok(applications);
    }
}