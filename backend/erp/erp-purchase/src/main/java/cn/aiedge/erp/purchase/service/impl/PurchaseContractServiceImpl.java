package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.erp.purchase.dto.ContractStatisticsDTO;
import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.entity.PurchaseContractItem;
import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.enums.ContractStatus;
import cn.aiedge.erp.purchase.mapper.PurchaseContractMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseContractItemMapper;
import cn.aiedge.erp.purchase.service.PurchaseContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 采购合同Service实现
 */
@Service
@RequiredArgsConstructor
public class PurchaseContractServiceImpl implements PurchaseContractService {

    private final PurchaseContractMapper contractMapper;
    private final PurchaseContractItemMapper itemMapper;

    @Override
    @Transactional
    public PurchaseContract generateContractFromQuote(PurchaseSupplierQuote quote, List<PurchaseContractItem> items) {
        PurchaseContract contract = new PurchaseContract();
        contract.setContractNo(generateContractNo());
        contract.setInquiryId(quote.getInquiryId());
        contract.setQuoteId(quote.getId());
        contract.setSupplierId(quote.getSupplierId());
        contract.setSupplierName(quote.getSupplierName());
        contract.setTotalAmount(quote.getTotalAmount());
        contract.setContractStatus(ContractStatus.DRAFT);
        contract.setCreatedBy(quote.getCreatedBy());
        contract.setCreatedAt(LocalDateTime.now());

        contractMapper.insert(contract);

        if (items != null && !items.isEmpty()) {
            items.forEach(item -> item.setContractId(contract.getId()));
            itemMapper.batchInsert(items);
        }

        return contract;
    }

    @Override
    @Transactional
    public PurchaseContract submitForApproval(Long id, String reason) {
        PurchaseContract contract = contractMapper.findById(id);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        contract.setContractStatus(ContractStatus.PENDING_APPROVAL);
        contract.setSubmitTime(LocalDateTime.now());
        contractMapper.updateStatus(id, ContractStatus.PENDING_APPROVAL.name(), LocalDateTime.now());

        return contractMapper.findById(id);
    }

    @Override
    @Transactional
    public PurchaseContract approveContract(Long id, Long approverId, String comment, boolean approved) {
        PurchaseContract contract = contractMapper.findById(id);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        ContractStatus newStatus = approved ? ContractStatus.APPROVED : ContractStatus.REJECTED;
        contract.setContractStatus(newStatus);
        contractMapper.updateStatus(id, newStatus.name(), LocalDateTime.now());
        contractMapper.updateApprovalInfo(id, approverId, comment, LocalDateTime.now());

        return contractMapper.findById(id);
    }

    @Override
    @Transactional
    public PurchaseContract activateContract(Long id) {
        PurchaseContract contract = contractMapper.findById(id);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        contract.setContractStatus(ContractStatus.ACTIVE);
        contractMapper.updateStatus(id, ContractStatus.ACTIVE.name(), LocalDateTime.now());
        contractMapper.updateActivationTime(id, LocalDateTime.now());

        return contractMapper.findById(id);
    }

    @Override
    @Transactional
    public PurchaseContract updateExecutionProgress(Long id, BigDecimal executedAmount, BigDecimal executedPercent) {
        PurchaseContract contract = contractMapper.findById(id);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        contractMapper.updateExecutionProgress(id, executedAmount, executedPercent);
        return contractMapper.findById(id);
    }

    @Override
    @Transactional
    public PurchaseContract completeContract(Long id) {
        PurchaseContract contract = contractMapper.findById(id);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        contract.setContractStatus(ContractStatus.COMPLETED);
        contractMapper.updateStatus(id, ContractStatus.COMPLETED.name(), LocalDateTime.now());
        contractMapper.updateCompletionTime(id, LocalDateTime.now());

        return contractMapper.findById(id);
    }

    @Override
    @Transactional
    public PurchaseContract terminateContract(Long id, String reason) {
        PurchaseContract contract = contractMapper.findById(id);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        contract.setContractStatus(ContractStatus.TERMINATED);
        contractMapper.updateStatus(id, ContractStatus.TERMINATED.name(), LocalDateTime.now());
        contractMapper.updateTerminationInfo(id, LocalDateTime.now(), reason);

        return contractMapper.findById(id);
    }

    @Override
    @Transactional
    public PurchaseContract archiveContract(Long id, String archiveNo, String reason) {
        PurchaseContract contract = contractMapper.findById(id);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        contract.setContractStatus(ContractStatus.ARCHIVED);
        contractMapper.updateStatus(id, ContractStatus.ARCHIVED.name(), LocalDateTime.now());
        contractMapper.updateArchiveInfo(id, archiveNo, LocalDateTime.now());

        return contractMapper.findById(id);
    }

    @Override
    public PurchaseContract getContractById(Long id) {
        return contractMapper.findById(id);
    }

    @Override
    public PurchaseContract getContractByNo(String contractNo) {
        return contractMapper.findByContractNo(contractNo);
    }

    @Override
    public List<PurchaseContract> queryContractsBySupplier(Long supplierId) {
        return contractMapper.findBySupplierId(supplierId);
    }

    @Override
    public List<PurchaseContract> queryContractsByStatus(ContractStatus status) {
        return contractMapper.findByStatus(status);
    }

    @Override
    public List<PurchaseContract> getContractsByInquiry(Long inquiryId) {
        return contractMapper.findByInquiryId(inquiryId);
    }

    @Override
    public List<PurchaseContractItem> getContractItems(Long contractId) {
        return itemMapper.findByContractId(contractId);
    }

    @Override
    public String generateContractNo() {
        String prefix = "CT";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        return prefix + "-" + datePart + "-" + randomPart;
    }

    @Override
    public BigDecimal calculateContractAmount(List<PurchaseContractItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
            .map(PurchaseContractItem::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean validateContractTerms(Long contractId) {
        PurchaseContract contract = contractMapper.findById(contractId);
        if (contract == null) {
            return false;
        }
        return contract.getPaymentTerms() != null && 
               contract.getDeliveryTerms() != null &&
               contract.getQualityStandard() != null &&
               contract.getWarrantyPeriod() != null;
    }

    @Override
    public List<PurchaseContract> queryExpiringContracts(int days) {
        return contractMapper.findExpiringContracts(days);
    }

    @Override
    @Transactional
    public PurchaseContract renewContract(Long id, int extendMonths, String reason) {
        PurchaseContract contract = contractMapper.findById(id);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        PurchaseContract newContract = new PurchaseContract();
        newContract.setContractNo(generateContractNo());
        newContract.setInquiryId(contract.getInquiryId());
        newContract.setQuoteId(contract.getQuoteId());
        newContract.setSupplierId(contract.getSupplierId());
        newContract.setSupplierName(contract.getSupplierName());
        newContract.setTotalAmount(contract.getTotalAmount());
        newContract.setContractStatus(ContractStatus.DRAFT);
        newContract.setStartDate(contract.getEndDate());
        newContract.setEndDate(contract.getEndDate().plusMonths(extendMonths));
        newContract.setPaymentTerms(contract.getPaymentTerms());
        newContract.setDeliveryTerms(contract.getDeliveryTerms());
        newContract.setQualityStandard(contract.getQualityStandard());
        newContract.setWarrantyPeriod(contract.getWarrantyPeriod());
        newContract.setCreatedAt(LocalDateTime.now());

        contractMapper.insert(newContract);
        return newContract;
    }

    @Override
    @Transactional
    public PurchaseContract modifyContract(Long id, String modificationReason, String detail) {
        PurchaseContract contract = contractMapper.findById(id);
        if (contract == null) {
            throw new IllegalArgumentException("合同不存在");
        }

        contract.setModificationNo("MOD-" + System.currentTimeMillis());
        contract.setModificationReason(modificationReason);
        contractMapper.insertModification(id, contract.getModificationNo(), modificationReason, LocalDateTime.now());
        contractMapper.update(contract);

        return contractMapper.findById(id);
    }

    @Override
    public ContractStatisticsDTO generateContractStatistics() {
        ContractStatisticsDTO stats = new ContractStatisticsDTO();
        stats.setActiveCount(contractMapper.countByStatus(ContractStatus.ACTIVE));
        stats.setCompletedCount(contractMapper.countByStatus(ContractStatus.COMPLETED));
        stats.setPendingCount(contractMapper.countByStatus(ContractStatus.PENDING_APPROVAL));
        stats.setActiveAmount(contractMapper.sumAmountByStatus(ContractStatus.ACTIVE));
        stats.setCompletedAmount(contractMapper.sumAmountByStatus(ContractStatus.COMPLETED));
        return stats;
    }
}