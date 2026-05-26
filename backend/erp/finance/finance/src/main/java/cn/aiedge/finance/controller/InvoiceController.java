package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.InvoiceDTO;
import cn.aiedge.finance.entity.Invoice;
import cn.aiedge.finance.service.InvoiceService;
import cn.aiedge.finance.vo.InvoiceQueryVO;
import cn.aiedge.finance.vo.InvoiceStatisticsVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 发票管理控制器
 */
@RestController
@RequestMapping("/api/finance/invoice")
@RequiredArgsConstructor
@Validated
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    
    /**
     * 创建发票
     */
    @PostMapping
    public Invoice create(@RequestBody @Validated InvoiceDTO invoiceDTO) {
        return invoiceService.createInvoice(invoiceDTO);
    }
    
    /**
     * 更新发票
     */
    @PutMapping("/{id}")
    public Invoice update(@PathVariable Long id, @RequestBody @Validated InvoiceDTO invoiceDTO) {
        return invoiceService.updateInvoice(id, invoiceDTO);
    }
    
    /**
     * 删除发票
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
    }
    
    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Invoice getById(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }
    
    /**
     * 根据发票号码查询
     */
    @GetMapping("/no/{invoiceNo}")
    public Invoice getByNo(@PathVariable String invoiceNo) {
        return invoiceService.getInvoiceByNo(invoiceNo);
    }
    
    /**
     * 分页查询
     */
    @GetMapping("/page")
    public IPage<Invoice> page(Page<Invoice> page, InvoiceQueryVO queryVO) {
        return invoiceService.pageInvoice(page, queryVO.toMap());
    }
    
    /**
     * 提交审核
     */
    @PostMapping("/{id}/submit")
    public Invoice submitForReview(@PathVariable Long id) {
        return invoiceService.submitForReview(id);
    }
    
    /**
     * 审核通过
     */
    @PostMapping("/{id}/approve")
    public Invoice approve(@PathVariable Long id, @RequestParam(required = false) String remark) {
        return invoiceService.approve(id, remark);
    }
    
    /**
     * 审核拒绝
     */
    @PostMapping("/{id}/reject")
    public Invoice reject(@PathVariable Long id, @RequestParam(required = false) String remark) {
        return invoiceService.reject(id, remark);
    }
    
    /**
     * 开具发票
     */
    @PostMapping("/{id}/issue")
    public Invoice issue(@PathVariable Long id) {
        return invoiceService.issueInvoice(id);
    }
    
    /**
     * 作废发票
     */
    @PostMapping("/{id}/void")
    public Invoice voidInvoice(@PathVariable Long id, @RequestParam String reason) {
        return invoiceService.voidInvoice(id, reason);
    }
    
    /**
     * 红冲发票
     */
    @PostMapping("/{id}/red")
    public Invoice redInvoice(@PathVariable Long id, @RequestParam String reason) {
        return invoiceService.redInvoice(id, reason);
    }
    
    /**
     * 认证发票
     */
    @PostMapping("/{id}/certify")
    public Invoice certify(@PathVariable Long id, @RequestParam Integer certifyMethod) {
        return invoiceService.certifyInvoice(id, certifyMethod);
    }
    
    /**
     * 核销发票
     */
    @PostMapping("/{id}/verify")
    public Invoice verify(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return invoiceService.verifyInvoice(id, amount);
    }
    
    /**
     * 打印发票
     */
    @PostMapping("/{id}/print")
    public Invoice print(@PathVariable Long id) {
        return invoiceService.printInvoice(id);
    }
    
    /**
     * 归档发票
     */
    @PostMapping("/{id}/archive")
    public Invoice archive(@PathVariable Long id) {
        return invoiceService.archiveInvoice(id);
    }
    
    /**
     * 获取待认证列表
     */
    @GetMapping("/pending-certify")
    public List<Invoice> getPendingCertifyList(InvoiceQueryVO queryVO) {
        return invoiceService.getPendingCertifyList(queryVO.getStartDate(), queryVO.getEndDate());
    }
    
    /**
     * 获取即将到期发票
     */
    @GetMapping("/expiring")
    public List<Invoice> getExpiringInvoices(@RequestParam(defaultValue = "30") Integer days) {
        return invoiceService.getExpiringInvoices(days);
    }
    
    /**
     * 统计发票金额
     */
    @GetMapping("/statistics/amount")
    public Map<String, BigDecimal> statisticsAmount(InvoiceQueryVO queryVO) {
        return invoiceService.statisticsAmount(queryVO.toMap());
    }
    
    /**
     * 按月统计
     */
    @GetMapping("/statistics/month")
    public List<Map<String, Object>> statisticsByMonth(InvoiceQueryVO queryVO) {
        return invoiceService.statisticsByMonth(queryVO.getStartDate(), queryVO.getEndDate(), queryVO.getDirection());
    }
    
    /**
     * 批量导入
     */
    @PostMapping("/batch-import")
    public List<Invoice> batchImport(@RequestBody List<InvoiceDTO> invoiceDTOList) {
        return invoiceService.batchImport(invoiceDTOList);
    }
}
