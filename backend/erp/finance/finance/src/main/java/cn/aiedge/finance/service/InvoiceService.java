package cn.aiedge.finance.service;

import cn.aiedge.finance.dto.InvoiceDTO;
import cn.aiedge.finance.entity.Invoice;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 发票服务接口
 */
public interface InvoiceService {
    
    /**
     * 创建发票
     */
    Invoice createInvoice(InvoiceDTO invoiceDTO);
    
    /**
     * 更新发票
     */
    Invoice updateInvoice(Long id, InvoiceDTO invoiceDTO);
    
    /**
     * 删除发票
     */
    void deleteInvoice(Long id);
    
    /**
     * 根据ID查询
     */
    Invoice getInvoiceById(Long id);
    
    /**
     * 根据发票号码查询
     */
    Invoice getInvoiceByNo(String invoiceNo);
    
    /**
     * 分页查询
     */
    IPage<Invoice> pageInvoice(Page<Invoice> page, Map<String, Object> param);
    
    /**
     * 提交审核
     */
    Invoice submitForReview(Long id);
    
    /**
     * 审核通过
     */
    Invoice approve(Long id, String remark);
    
    /**
     * 审核拒绝
     */
    Invoice reject(Long id, String remark);
    
    /**
     * 开具发票
     */
    Invoice issueInvoice(Long id);
    
    /**
     * 作废发票
     */
    Invoice voidInvoice(Long id, String reason);
    
    /**
     * 红冲发票
     */
    Invoice redInvoice(Long id, String reason);
    
    /**
     * 认证发票
     */
    Invoice certifyInvoice(Long id, Integer certifyMethod);
    
    /**
     * 核销发票
     */
    Invoice verifyInvoice(Long id, BigDecimal amount);
    
    /**
     * 打印发票
     */
    Invoice printInvoice(Long id);
    
    /**
     * 归档发票
     */
    Invoice archiveInvoice(Long id);
    
    /**
     * 获取待认证列表
     */
    List<Invoice> getPendingCertifyList(LocalDate startDate, LocalDate endDate);
    
    /**
     * 获取即将到期发票
     */
    List<Invoice> getExpiringInvoices(Integer days);
    
    /**
     * 统计发票金额
     */
    Map<String, BigDecimal> statisticsAmount(Map<String, Object> param);
    
    /**
     * 按月统计
     */
    List<Map<String, Object>> statisticsByMonth(LocalDate startDate, LocalDate endDate, Integer direction);
    
    /**
     * 自动匹配采购订单
     */
    Invoice autoMatchPurchaseOrder(Long invoiceId);
    
    /**
     * 自动匹配销售订单
     */
    Invoice autoMatchSaleOrder(Long invoiceId);
    
    /**
     * 批量导入发票
     */
    List<Invoice> batchImport(List<InvoiceDTO> invoiceDTOList);
}
