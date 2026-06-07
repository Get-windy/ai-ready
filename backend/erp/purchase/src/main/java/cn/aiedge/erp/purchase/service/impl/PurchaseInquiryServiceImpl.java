package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.purchase.entity.PurchaseInquiry;
import cn.aiedge.erp.purchase.enums.InquiryStatus;
import cn.aiedge.erp.purchase.mapper.PurchaseInquiryMapper;
import cn.aiedge.erp.purchase.service.PurchaseInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 询价单Service实现
 */
@Service
@RequiredArgsConstructor
public class PurchaseInquiryServiceImpl implements PurchaseInquiryService {

    private final PurchaseInquiryMapper inquiryMapper;

    @Override
    @Transactional
    public PurchaseInquiry createInquiry(PurchaseInquiry inquiry) {
        inquiry.setInquiryNo(generateInquiryNo());
        inquiry.setStatus(InquiryStatus.DRAFT);
        inquiryMapper.insert(inquiry);
        return inquiry;
    }

    @Override
    @Transactional
    public PurchaseInquiry updateInquiry(Long id, PurchaseInquiry inquiry) {
        PurchaseInquiry existing = inquiryMapper.findById(id);
        if (existing == null) {
            throw BusinessException.notFound("询价单不存在");
        }
        inquiry.setId(id);
        inquiryMapper.update(inquiry);
        return inquiryMapper.findById(id);
    }

    @Override
    @Transactional
    public PurchaseInquiry publishInquiry(Long id) {
        PurchaseInquiry inquiry = inquiryMapper.findById(id);
        if (inquiry == null) {
            throw BusinessException.notFound("询价单不存在");
        }
        inquiry.setStatus(InquiryStatus.PUBLISHED);
        inquiry.setPublishDate(LocalDateTime.now());
        inquiryMapper.updateStatus(id, InquiryStatus.PUBLISHED.name(), inquiry.getPublishDate().toString());
        return inquiry;
    }

    @Override
    @Transactional
    public PurchaseInquiry closeInquiry(Long id) {
        return closeInquiry(id, null);
    }

    @Override
    @Transactional
    public PurchaseInquiry closeInquiry(Long id, String reason) {
        PurchaseInquiry inquiry = inquiryMapper.findById(id);
        if (inquiry == null) {
            throw BusinessException.notFound("询价单不存在");
        }
        inquiry.setStatus(InquiryStatus.CLOSED);
        inquiry.setCloseDate(LocalDateTime.now());
        inquiryMapper.updateStatus(id, InquiryStatus.CLOSED.name(), inquiry.getCloseDate().toString());
        return inquiry;
    }

    @Override
    public boolean canAcceptQuote(Long id) {
        PurchaseInquiry inquiry = inquiryMapper.findById(id);
        if (inquiry == null) {
            throw new IllegalArgumentException("询价单不存在");
        }
        if (!InquiryStatus.PUBLISHED.equals(inquiry.getStatus())) {
            return false;
        }
        return inquiry.getDeadlineDate() == null || inquiry.getDeadlineDate().isAfter(LocalDateTime.now());
    }

    @Override
    @Transactional
    public PurchaseInquiry cancelInquiry(Long id) {
        PurchaseInquiry inquiry = inquiryMapper.findById(id);
        if (inquiry == null) {
            throw BusinessException.notFound("询价单不存在");
        }
        inquiry.setStatus(InquiryStatus.CANCELLED);
        inquiryMapper.updateStatus(id, InquiryStatus.CANCELLED.name(), null);
        return inquiry;
    }

    @Override
    public PurchaseInquiry getInquiryById(Long id) {
        return inquiryMapper.findById(id);
    }

    @Override
    public PurchaseInquiry getInquiryByNo(String inquiryNo) {
        return inquiryMapper.findByInquiryNo(inquiryNo);
    }

    @Override
    public List<PurchaseInquiry> getAllInquiries() {
        return inquiryMapper.findAll();
    }

    @Override
    public List<PurchaseInquiry> getInquiriesByStatus(String status) {
        return inquiryMapper.findByStatus(status);
    }

    @Override
    public List<PurchaseInquiry> getInquiriesByPurchaser(Long purchaserId) {
        return inquiryMapper.findByPurchaserId(purchaserId);
    }

    @Override
    @Transactional
    public void deleteInquiry(Long id) {
        inquiryMapper.deleteById(id);
    }

    @Override
    public String generateInquiryNo() {
        String prefix = "INQ";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int)(Math.random() * 10000));
        return prefix + datePart + randomPart;
    }

    @Transactional
    public void updateQuoteCount(Long inquiryId) {
        PurchaseInquiry inquiry = inquiryMapper.findById(inquiryId);
        if (inquiry == null) {
            throw new IllegalArgumentException("询价单不存在");
        }
    }
}