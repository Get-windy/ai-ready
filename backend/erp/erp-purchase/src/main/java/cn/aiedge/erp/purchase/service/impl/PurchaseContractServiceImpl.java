package cn.aiedge.erp.purchase.service.impl;

import cn.aiedge.base.config.MyBatisPlusConfig;
import cn.aiedge.erp.purchase.dto.ContractStatisticsDTO;
import cn.aiedge.erp.purchase.dto.PurchaseContractQueryDTO;
import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.entity.PurchaseContractItem;
import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.enums.ContractStatus;
import cn.aiedge.erp.purchase.mapper.PurchaseContractMapper;
import cn.aiedge.erp.purchase.mapper.PurchaseContractItemMapper;
import cn.aiedge.erp.purchase.service.PurchaseContractService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        // 前端统计卡片要的是「合同总数 / 草稿 / 待审批 / 生效中 / 已完成 / 合同总金额」，
        // 原实现漏了 totalCount、draftCount、totalAmount 三项 ⇒ 卡片恒为 0。
        stats.setTotalCount(contractMapper.countAll());
        stats.setDraftCount(contractMapper.countByStatus(ContractStatus.DRAFT));
        stats.setActiveCount(contractMapper.countByStatus(ContractStatus.ACTIVE));
        stats.setCompletedCount(contractMapper.countByStatus(ContractStatus.COMPLETED));
        stats.setPendingCount(contractMapper.countByStatus(ContractStatus.PENDING_APPROVAL));
        stats.setTotalAmount(contractMapper.sumAllAmount());
        stats.setActiveAmount(contractMapper.sumAmountByStatus(ContractStatus.ACTIVE));
        stats.setCompletedAmount(contractMapper.sumAmountByStatus(ContractStatus.COMPLETED));
        return stats;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 合同列表页（菜单 81010）CRUD —— 2026-09-21 补齐
    //
    // 此前该页调用的 4 个端点在后端**都不存在**，且 `purchase_contract` 表本身也缺失，
    // 属「菜单在、页面在、接口缺、表缺」的四重断点（PUR-BREAK-01）。
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public Page<PurchaseContract> pageContracts(PurchaseContractQueryDTO query) {
        long current = (query.getCurrent() == null || query.getCurrent() < 1) ? 1L : query.getCurrent();
        // 上限 200：避免前端传 size=999999 把整表拉进内存
        long size = (query.getSize() == null || query.getSize() < 1) ? 10L : Math.min(query.getSize(), 200L);
        long offset = (current - 1) * size;

        List<PurchaseContract> records = contractMapper.selectPageList(
                query.getContractNo(), query.getContractTitle(), query.getSupplierName(),
                query.getSupplierId(), query.getContractStatus(),
                query.getDateStart(), query.getDateEnd(), offset, size);
        long total = contractMapper.countPageList(
                query.getContractNo(), query.getContractTitle(), query.getSupplierName(),
                query.getSupplierId(), query.getContractStatus(),
                query.getDateStart(), query.getDateEnd());

        Page<PurchaseContract> page = new Page<>(current, size);
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @Override
    @Transactional
    public PurchaseContract createContract(PurchaseContract contract) {
        if (contract.getContractTitle() == null || contract.getContractTitle().isBlank()) {
            throw new IllegalArgumentException("合同标题不能为空");
        }
        if (contract.getSupplierId() == null) {
            throw new IllegalArgumentException("供应商不能为空");
        }
        if (contract.getContractNo() == null || contract.getContractNo().isBlank()) {
            contract.setContractNo(generateContractNo());
        }
        // 新增一律从草稿开始：状态由审批流推进，不接受前端直接传 ACTIVE/APPROVED
        contract.setContractStatus(ContractStatus.DRAFT);
        if (contract.getTotalAmount() == null) {
            contract.setTotalAmount(BigDecimal.ZERO);
        }
        contract.setExecutedAmount(BigDecimal.ZERO);
        contract.setExecutedPercent(BigDecimal.ZERO);
        contract.setCreatedAt(LocalDateTime.now());
        // ⚠️ 本 Mapper 是自定义 @Insert 注解 SQL，**不经过 MyBatis-Plus 的 insertFill**
        //    ⇒ 租户必须显式写入。漏写不会报错，但会落成 tenant_id = 0 的「谁都不看不见」数据。
        if (contract.getTenantId() == null) {
            contract.setTenantId(MyBatisPlusConfig.getCurrentTenantIdValue());
        }
        contractMapper.insert(contract);
        return contractMapper.findById(contract.getId());
    }

    @Override
    @Transactional
    public PurchaseContract updateContract(Long id, PurchaseContract contract) {
        PurchaseContract existing = contractMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("合同不存在");
        }
        if (existing.getContractStatus() != ContractStatus.DRAFT
                && existing.getContractStatus() != ContractStatus.REJECTED) {
            throw new IllegalStateException("只有草稿或已驳回状态的合同可以编辑");
        }
        contract.setId(id);
        // 编号与状态不由编辑接口改写（状态只能经 submit/approve/terminate 等流转）
        contract.setContractNo(null);
        contract.setContractStatus(null);
        contractMapper.update(contract);
        return contractMapper.findById(id);
    }

    @Override
    @Transactional
    public void deleteContract(Long id) {
        PurchaseContract existing = contractMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("合同不存在");
        }
        if (existing.getContractStatus() != ContractStatus.DRAFT) {
            throw new IllegalStateException("只有草稿状态的合同可以删除");
        }
        // 先删明细再删主表：本表无外键约束，不做这一步会留下孤儿明细
        itemMapper.deleteByContractId(id);
        contractMapper.deleteById(id);
    }
}