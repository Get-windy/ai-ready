package cn.aiedge.erp.invoice.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.invoice.model.entity.InvoiceApplication;
import cn.aiedge.erp.invoice.model.enums.InvoiceStatus;
import cn.aiedge.erp.invoice.repository.InvoiceApplicationRepository;
import cn.aiedge.erp.invoice.service.InvoiceApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 发票申请服务实现类
 */
@Service
@Transactional
public class InvoiceApplicationServiceImpl implements InvoiceApplicationService {

    @Autowired
    private InvoiceApplicationRepository invoiceApplicationRepository;

    @Override
    public InvoiceApplication createInvoiceApplication(InvoiceApplication application) {
        // 设置申请单号
        if (application.getApplicationNumber() == null || application.getApplicationNumber().trim().isEmpty()) {
            application.setApplicationNumber(generateApplicationNumber());
        }
        
        // 设置申请日期
        if (application.getApplicationDate() == null) {
            application.setApplicationDate(LocalDate.now());
        }
        
        // 设置状态为草稿
        if (application.getStatus() == null) {
            application.setStatus(InvoiceStatus.DRAFT);
        }
        
        return invoiceApplicationRepository.save(application);
    }

    @Override
    public Optional<InvoiceApplication> getInvoiceApplicationById(Long id) {
        return invoiceApplicationRepository.findById(id);
    }

    @Override
    public List<InvoiceApplication> getAllInvoiceApplications() {
        return invoiceApplicationRepository.findAll();
    }

    @Override
    public Page<InvoiceApplication> getInvoiceApplications(Pageable pageable) {
        return invoiceApplicationRepository.findAll(pageable);
    }

    @Override
    public List<InvoiceApplication> getInvoiceApplicationsByStatus(InvoiceStatus status) {
        return invoiceApplicationRepository.findByStatus(status);
    }

    @Override
    public List<InvoiceApplication> getInvoiceApplicationsByCustomerId(Long customerId) {
        return invoiceApplicationRepository.findByCustomerId(customerId);
    }

    @Override
    public List<InvoiceApplication> getInvoiceApplicationsBySupplierId(Long supplierId) {
        return invoiceApplicationRepository.findBySupplierId(supplierId);
    }

    @Override
    public List<InvoiceApplication> getInvoiceApplicationsByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceApplicationRepository.findByApplicationDateBetween(startDate, endDate);
    }

    @Override
    public InvoiceApplication updateInvoiceApplication(Long id, InvoiceApplication application) {
        Optional<InvoiceApplication> existing = invoiceApplicationRepository.findById(id);
        if (existing.isPresent()) {
            InvoiceApplication existingApp = existing.get();
            
            // 更新可编辑字段
            existingApp.setCustomerId(application.getCustomerId());
            existingApp.setSupplierId(application.getSupplierId());
            existingApp.setInvoiceType(application.getInvoiceType());
            existingApp.setCurrencyCode(application.getCurrencyCode());
            existingApp.setExchangeRate(application.getExchangeRate());
            existingApp.setSubtotalAmount(application.getSubtotalAmount());
            existingApp.setTaxAmount(application.getTaxAmount());
            existingApp.setDiscountAmount(application.getDiscountAmount());
            existingApp.setShippingAmount(application.getShippingAmount());
            existingApp.setOtherAmount(application.getOtherAmount());
            existingApp.setTotalAmount(application.getTotalAmount());
            existingApp.setExpectedInvoiceDate(application.getExpectedInvoiceDate());
            existingApp.setExpectedPaymentDate(application.getExpectedPaymentDate());
            existingApp.setPaymentTerms(application.getPaymentTerms());
            existingApp.setDeliveryTerms(application.getDeliveryTerms());
            existingApp.setShippingMethod(application.getShippingMethod());
            existingApp.setShippingAddress(application.getShippingAddress());
            existingApp.setBillingAddress(application.getBillingAddress());
            existingApp.setContactPerson(application.getContactPerson());
            existingApp.setContactPhone(application.getContactPhone());
            existingApp.setContactEmail(application.getContactEmail());
            existingApp.setNotes(application.getNotes());
            existingApp.setAttachmentPaths(application.getAttachmentPaths());
            existingApp.setBusinessRegion(application.getBusinessRegion());
            existingApp.setBusinessDepartment(application.getBusinessDepartment());
            existingApp.setProjectCode(application.getProjectCode());
            existingApp.setContractNumber(application.getContractNumber());
            existingApp.setIsUrgent(application.getIsUrgent());
            existingApp.setPriorityLevel(application.getPriorityLevel());
            
            return invoiceApplicationRepository.save(existingApp);
        }
        throw BusinessException.notFound("发票申请不存在");
    }

    @Override
    public boolean deleteInvoiceApplication(Long id, String deletedBy) {
        Optional<InvoiceApplication> optional = invoiceApplicationRepository.findById(id);
        if (optional.isPresent()) {
            InvoiceApplication application = optional.get();
            application.softDelete(deletedBy);
            invoiceApplicationRepository.save(application);
            return true;
        }
        return false;
    }

    @Override
    public boolean submitInvoiceApplication(Long id, Long submittedBy) {
        Optional<InvoiceApplication> optional = invoiceApplicationRepository.findById(id);
        if (optional.isPresent()) {
            InvoiceApplication application = optional.get();
            if (application.canSubmit()) {
                application.setStatus(InvoiceStatus.SUBMITTED);
                application.setSubmittedAt(java.time.LocalDateTime.now());
                application.setSubmittedBy(submittedBy);
                invoiceApplicationRepository.save(application);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean approveInvoiceApplication(Long id, Long approvedBy, String notes) {
        Optional<InvoiceApplication> optional = invoiceApplicationRepository.findById(id);
        if (optional.isPresent()) {
            InvoiceApplication application = optional.get();
            if (application.canApprove()) {
                application.setStatus(InvoiceStatus.APPROVED);
                application.setApprovedAt(java.time.LocalDateTime.now());
                application.setApprovedBy(approvedBy);
                application.setApprovalNotes(notes);
                invoiceApplicationRepository.save(application);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean rejectInvoiceApplication(Long id, Long rejectedBy, String reason) {
        Optional<InvoiceApplication> optional = invoiceApplicationRepository.findById(id);
        if (optional.isPresent()) {
            InvoiceApplication application = optional.get();
            if (application.canReject()) {
                application.setStatus(InvoiceStatus.REJECTED);
                application.setRejectedAt(java.time.LocalDateTime.now());
                application.setRejectedBy(rejectedBy);
                application.setRejectionReason(reason);
                invoiceApplicationRepository.save(application);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean cancelInvoiceApplication(Long id, Long cancelledBy, String reason) {
        Optional<InvoiceApplication> optional = invoiceApplicationRepository.findById(id);
        if (optional.isPresent()) {
            InvoiceApplication application = optional.get();
            if (application.canCancel()) {
                application.setStatus(InvoiceStatus.CANCELLED);
                application.setRejectedAt(java.time.LocalDateTime.now());
                application.setRejectedBy(cancelledBy);
                application.setRejectionReason(reason);
                invoiceApplicationRepository.save(application);
                return true;
            }
        }
        return false;
    }

    @Override
    public Long generateInvoiceFromApplication(Long id, Long issuedBy, String issuedByName) {
        Optional<InvoiceApplication> optional = invoiceApplicationRepository.findById(id);
        if (optional.isPresent()) {
            InvoiceApplication application = optional.get();
            if (application.canGenerateInvoice()) {
                // 这里应该调用发票服务来生成发票
                // 为了简化，我们只更新申请状态
                application.setInvoiceGenerated(true);
                // application.setInvoiceId(invoiceId); // 实际实现中需要设置生成的发票ID
                invoiceApplicationRepository.save(application);
                return application.getId(); // 返回申请ID作为占位符
            }
        }
        return null;
    }

    @Override
    public boolean validateInvoiceApplication(InvoiceApplication application) {
        return application.validate();
    }

    @Override
    public List<InvoiceApplication> searchInvoiceApplications(String keyword) {
        return invoiceApplicationRepository.searchInvoiceApplications(keyword);
    }

    /**
     * 生成申请单号
     */
    private String generateApplicationNumber() {
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequencePart = String.format("%05d", (int)(Math.random() * 100000));
        return "APP-" + datePart + "-" + sequencePart;
    }
}