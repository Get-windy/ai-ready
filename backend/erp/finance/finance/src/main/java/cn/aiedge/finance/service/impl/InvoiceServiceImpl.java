package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.dto.InvoiceDTO;
import cn.aiedge.finance.dto.InvoiceItemDTO;
import cn.aiedge.finance.entity.Invoice;
import cn.aiedge.finance.entity.InvoiceFlow;
import cn.aiedge.finance.entity.InvoiceItem;
import cn.aiedge.finance.repository.InvoiceRepository;
import cn.aiedge.finance.service.InvoiceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 发票服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl extends ServiceImpl<InvoiceRepository, Invoice> implements InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice createInvoice(InvoiceDTO invoiceDTO) {
        // 校验发票号码唯一性
        Invoice existInvoice = invoiceRepository.selectByInvoiceNo(invoiceDTO.getInvoiceNo());
        if (existInvoice != null) {
            throw new RuntimeException("发票号码已存在");
        }
        
        // 计算金额
        calculateAmount(invoiceDTO);
        
        // 创建发票
        Invoice invoice = new Invoice();
        BeanUtils.copyProperties(invoiceDTO, invoice);
        invoice.setStatus(0); // 草稿状态
        invoice.setVerifiedAmount(BigDecimal.ZERO);
        invoice.setUnverifiedAmount(invoiceDTO.getTotalAmount());
        invoice.setCertified(0);
        invoice.setPrinted(0);
        invoice.setPrintCount(0);
        
        invoiceRepository.insert(invoice);
        
        // 保存明细
        saveInvoiceItems(invoice.getId(), invoiceDTO.getItems());
        
        // 记录流转
        saveInvoiceFlow(invoice.getId(), 1, null, 0, "创建发票");
        
        log.info("创建发票成功: {}", invoice.getInvoiceNo());
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice updateInvoice(Long id, InvoiceDTO invoiceDTO) {
        Invoice invoice = getById(id);
        if (invoice == null) {
            throw new RuntimeException("发票不存在");
        }
        
        // 只有草稿状态可以修改
        if (invoice.getStatus() != 0) {
            throw new RuntimeException("只有草稿状态的发票可以修改");
        }
        
        // 计算金额
        calculateAmount(invoiceDTO);
        
        BeanUtils.copyProperties(invoiceDTO, invoice);
        invoice.setUnverifiedAmount(invoiceDTO.getTotalAmount());
        
        invoiceRepository.updateById(invoice);
        
        // 更新明细
        saveInvoiceItems(invoice.getId(), invoiceDTO.getItems());
        
        log.info("更新发票成功: {}", invoice.getInvoiceNo());
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInvoice(Long id) {
        Invoice invoice = getById(id);
        if (invoice == null) {
            throw new RuntimeException("发票不存在");
        }
        
        if (invoice.getStatus() > 2) {
            throw new RuntimeException("已开具的发票不能删除");
        }
        
        invoiceRepository.deleteById(id);
        log.info("删除发票成功: {}", invoice.getInvoiceNo());
    }
    
    @Override
    public Invoice getInvoiceById(Long id) {
        return getById(id);
    }
    
    @Override
    public Invoice getInvoiceByNo(String invoiceNo) {
        return invoiceRepository.selectByInvoiceNo(invoiceNo);
    }
    
    @Override
    public IPage<Invoice> pageInvoice(Page<Invoice> page, Map<String, Object> param) {
        return invoiceRepository.selectInvoicePage(page, param);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice submitForReview(Long id) {
        Invoice invoice = getById(id);
        if (invoice == null || invoice.getStatus() != 0) {
            throw new RuntimeException("发票不存在或状态错误");
        }
        
        invoice.setStatus(1); // 待审核
        invoiceRepository.updateById(invoice);
        
        saveInvoiceFlow(id, 2, 0, 1, "提交审核");
        
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice approve(Long id, String remark) {
        Invoice invoice = getById(id);
        if (invoice == null || invoice.getStatus() != 1) {
            throw new RuntimeException("发票不存在或状态错误");
        }
        
        invoice.setStatus(2); // 已审核
        invoiceRepository.updateById(invoice);
        
        saveInvoiceFlow(id, 3, 1, 2, "审核通过: " + remark);
        
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice reject(Long id, String remark) {
        Invoice invoice = getById(id);
        if (invoice == null || invoice.getStatus() != 1) {
            throw new RuntimeException("发票不存在或状态错误");
        }
        
        invoice.setStatus(0); // 退回草稿
        invoiceRepository.updateById(invoice);
        
        saveInvoiceFlow(id, 4, 1, 0, "审核拒绝: " + remark);
        
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice issueInvoice(Long id) {
        Invoice invoice = getById(id);
        if (invoice == null || invoice.getStatus() != 2) {
            throw new RuntimeException("发票不存在或状态错误");
        }
        
        invoice.setStatus(3); // 已开具
        invoiceRepository.updateById(invoice);
        
        saveInvoiceFlow(id, 5, 2, 3, "开具发票");
        
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice voidInvoice(Long id, String reason) {
        Invoice invoice = getById(id);
        if (invoice == null || invoice.getStatus() != 3) {
            throw new RuntimeException("发票不存在或状态错误");
        }
        
        invoice.setStatus(4); // 已作废
        invoice.setVoidReason(reason);
        invoiceRepository.updateById(invoice);
        
        saveInvoiceFlow(id, 6, 3, 4, "作废发票: " + reason);
        
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice redInvoice(Long id, String reason) {
        Invoice originalInvoice = getById(id);
        if (originalInvoice == null || originalInvoice.getStatus() != 3) {
            throw new RuntimeException("原发票不存在或状态错误");
        }
        
        // 创建红冲发票
        Invoice redInvoice = new Invoice();
        BeanUtils.copyProperties(originalInvoice, redInvoice);
        redInvoice.setId(null);
        redInvoice.setInvoiceNo(generateRedInvoiceNo());
        redInvoice.setStatus(5); // 已红冲
        redInvoice.setRedReason(reason);
        redInvoice.setOriginalInvoiceId(originalInvoice.getId());
        redInvoice.setOriginalInvoiceNo(originalInvoice.getInvoiceNo());
        redInvoice.setAmount(originalInvoice.getAmount().negate());
        redInvoice.setTaxAmount(originalInvoice.getTaxAmount().negate());
        redInvoice.setTotalAmount(originalInvoice.getTotalAmount().negate());
        
        invoiceRepository.insert(redInvoice);
        
        // 更新原发票状态
        originalInvoice.setStatus(5);
        invoiceRepository.updateById(originalInvoice);
        
        saveInvoiceFlow(id, 7, 3, 5, "红冲发票: " + reason);
        
        return redInvoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice certifyInvoice(Long id, Integer certifyMethod) {
        Invoice invoice = getById(id);
        if (invoice == null || invoice.getStatus() != 3) {
            throw new RuntimeException("发票不存在或状态错误");
        }
        
        invoice.setCertified(1);
        invoice.setCertifyDate(LocalDate.now());
        invoice.setCertifyMethod(certifyMethod);
        invoiceRepository.updateById(invoice);
        
        saveInvoiceFlow(id, 10, invoice.getStatus(), invoice.getStatus(), "认证发票");
        
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice verifyInvoice(Long id, BigDecimal amount) {
        Invoice invoice = getById(id);
        if (invoice == null) {
            throw new RuntimeException("发票不存在");
        }
        
        BigDecimal newVerifiedAmount = invoice.getVerifiedAmount().add(amount);
        if (newVerifiedAmount.compareTo(invoice.getTotalAmount()) > 0) {
            throw new RuntimeException("核销金额不能超过发票金额");
        }
        
        invoice.setVerifiedAmount(newVerifiedAmount);
        invoice.setUnverifiedAmount(invoice.getTotalAmount().subtract(newVerifiedAmount));
        invoiceRepository.updateById(invoice);
        
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice printInvoice(Long id) {
        Invoice invoice = getById(id);
        if (invoice == null || invoice.getStatus() != 3) {
            throw new RuntimeException("发票不存在或状态错误");
        }
        
        invoice.setPrinted(1);
        invoice.setPrintCount(invoice.getPrintCount() + 1);
        invoiceRepository.updateById(invoice);
        
        saveInvoiceFlow(id, 9, 3, 3, "打印发票");
        
        return invoice;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice archiveInvoice(Long id) {
        Invoice invoice = getById(id);
        if (invoice == null || invoice.getStatus() != 3) {
            throw new RuntimeException("发票不存在或状态错误");
        }
        
        invoice.setStatus(6); // 已归档
        invoiceRepository.updateById(invoice);
        
        saveInvoiceFlow(id, 8, 3, 6, "归档发票");
        
        return invoice;
    }
    
    @Override
    public List<Invoice> getPendingCertifyList(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.selectPendingCertifyList(startDate, endDate);
    }
    
    @Override
    public List<Invoice> getExpiringInvoices(Integer days) {
        return invoiceRepository.selectExpiringInvoices(days);
    }
    
    @Override
    public Map<String, BigDecimal> statisticsAmount(Map<String, Object> param) {
        return invoiceRepository.statisticsAmount(param);
    }
    
    @Override
    public List<Map<String, Object>> statisticsByMonth(LocalDate startDate, LocalDate endDate, Integer direction) {
        return invoiceRepository.statisticsByMonth(startDate, endDate, direction);
    }
    
    @Override
    public Invoice autoMatchPurchaseOrder(Long invoiceId) {
        // TODO: 实现自动匹配采购订单逻辑
        return getById(invoiceId);
    }
    
    @Override
    public Invoice autoMatchSaleOrder(Long invoiceId) {
        // TODO: 实现自动匹配销售订单逻辑
        return getById(invoiceId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Invoice> batchImport(List<InvoiceDTO> invoiceDTOList) {
        return invoiceDTOList.stream()
                .map(this::createInvoice)
                .toList();
    }
    
    // ========== 私有方法 ==========
    
    private void calculateAmount(InvoiceDTO invoiceDTO) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        
        for (InvoiceItemDTO item : invoiceDTO.getItems()) {
            BigDecimal amount = item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = amount.multiply(item.getTaxRate()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemTotal = amount.add(taxAmount);
            
            item.setAmount(amount);
            item.setTaxAmount(taxAmount);
            item.setTotalAmount(itemTotal);
            
            totalAmount = totalAmount.add(amount);
            totalTaxAmount = totalTaxAmount.add(taxAmount);
        }
        
        invoiceDTO.setAmount(totalAmount);
        invoiceDTO.setTaxAmount(totalTaxAmount);
        invoiceDTO.setTotalAmount(totalAmount.add(totalTaxAmount));
    }
    
    private void saveInvoiceItems(Long invoiceId, List<InvoiceItemDTO> items) {
        // 先删除原有明细
        LambdaQueryWrapper<InvoiceItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvoiceItem::getInvoiceId, invoiceId);
        // TODO: 调用itemMapper删除
        
        // 保存新明细
        int lineNo = 1;
        for (InvoiceItemDTO itemDTO : items) {
            InvoiceItem item = new InvoiceItem();
            BeanUtils.copyProperties(itemDTO, item);
            item.setInvoiceId(invoiceId);
            item.setLineNo(lineNo++);
            // TODO: 调用itemMapper插入
        }
    }
    
    private void saveInvoiceFlow(Long invoiceId, Integer operationType, 
                                  Integer beforeStatus, Integer afterStatus, String remark) {
        InvoiceFlow flow = new InvoiceFlow();
        flow.setInvoiceId(invoiceId);
        flow.setOperationType(operationType);
        flow.setBeforeStatus(beforeStatus);
        flow.setAfterStatus(afterStatus);
        flow.setRemark(remark);
        // TODO: 调用flowMapper插入
    }
    
    private String generateRedInvoiceNo() {
        return "RED" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) 
                + UUID.randomUUID().toString().substring(0, 4);
    }
}
