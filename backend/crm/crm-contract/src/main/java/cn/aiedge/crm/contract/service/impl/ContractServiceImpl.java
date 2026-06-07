package cn.aiedge.crm.contract.service.impl;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.crm.contract.entity.Contract;
import cn.aiedge.crm.contract.entity.ContractAttachment;
import cn.aiedge.crm.contract.entity.ContractChange;
import cn.aiedge.crm.contract.entity.ContractClause;
import cn.aiedge.crm.contract.entity.ContractPayment;
import cn.aiedge.crm.contract.enums.ContractStatus;
import cn.aiedge.crm.contract.mapper.ContractAttachmentMapper;
import cn.aiedge.crm.contract.mapper.ContractChangeMapper;
import cn.aiedge.crm.contract.mapper.ContractClauseMapper;
import cn.aiedge.crm.contract.mapper.ContractMapper;
import cn.aiedge.crm.contract.mapper.ContractPaymentMapper;
import cn.aiedge.crm.contract.service.ContractService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements ContractService {

    private final ContractClauseMapper clauseMapper;
    private final ContractAttachmentMapper attachmentMapper;
    private final ContractPaymentMapper paymentMapper;
    private final ContractChangeMapper changeMapper;

    @Override
    public Contract getByContractNo(String contractNo) {
        return lambdaQuery()
                .eq(Contract::getContractNo, contractNo)
                .eq(Contract::getDeleted, 0)
                .one();
    }

    @Override
    public Page<Contract> pageList(String keyword, Long customerId, Long opportunityId, Integer status, Integer contractType, Long salesPersonId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Contract> wrapper = buildQueryWrapper(keyword, customerId, opportunityId, status, contractType, salesPersonId);
        wrapper.orderByDesc(Contract::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<Contract> exportList(String keyword, Long customerId, Long opportunityId, Integer status, Integer contractType, Long salesPersonId) {
        LambdaQueryWrapper<Contract> wrapper = buildQueryWrapper(keyword, customerId, opportunityId, status, contractType, salesPersonId);
        wrapper.orderByDesc(Contract::getCreateTime);
        return baseMapper.selectList(wrapper);
    }

    /**
     * 构建公共查询条件
     */
    private LambdaQueryWrapper<Contract> buildQueryWrapper(String keyword, Long customerId, Long opportunityId,
                                                            Integer status, Integer contractType, Long salesPersonId) {
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contract::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Contract::getContractNo, keyword)
                    .or().like(Contract::getContractName, keyword)
                    .or().like(Contract::getCustomerName, keyword));
        }
        if (customerId != null) {
            wrapper.eq(Contract::getCustomerId, customerId);
        }
        if (opportunityId != null) {
            wrapper.eq(Contract::getOpportunityId, opportunityId);
        }
        if (status != null) {
            wrapper.eq(Contract::getStatus, status);
        }
        if (contractType != null) {
            wrapper.eq(Contract::getContractType, contractType);
        }
        if (salesPersonId != null) {
            wrapper.eq(Contract::getSalesPersonId, salesPersonId);
        }
        return wrapper;
    }

    @Override
    public List<Contract> listByCustomerId(Long customerId) {
        return baseMapper.selectByCustomerId(customerId);
    }

    @Override
    public List<Contract> listByOpportunityId(Long opportunityId) {
        return baseMapper.selectByOpportunityId(opportunityId);
    }

    @Override
    public List<Contract> listBySalesPersonId(Long salesPersonId) {
        return baseMapper.selectBySalesPersonId(salesPersonId);
    }

    @Override
    public List<Contract> listByStatus(Integer status) {
        return baseMapper.selectByStatus(status, 1L);
    }

    @Override
    public String generateContractNo() {
        String prefix = "CT";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<Contract> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Contract::getContractNo, prefix + dateStr)
                .eq(Contract::getDeleted, 0)
                .orderByDesc(Contract::getContractNo)
                .last("LIMIT 1");
        Contract lastContract = getOne(wrapper);
        int seq = 1;
        if (lastContract != null) {
            String lastNo = lastContract.getContractNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        return prefix + dateStr + String.format("%04d", seq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract createContract(Contract contract) {
        contract.setContractNo(generateContractNo());
        contract.setStatus(ContractStatus.DRAFT.getCode());
        contract.setExecutionProgress(0);
        contract.setExecutionAmount(BigDecimal.ZERO);
        contract.setPaidAmount(BigDecimal.ZERO);
        contract.setPendingAmount(contract.getContractAmount());
        contract.setRenewalCount(0);
        save(contract);
        return getById(contract.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract createFromQuotation(Long quotationId) {
        Contract contract = new Contract();
        contract.setContractName("销售合同");
        contract.setContractType(1);
        contract.setQuotationId(quotationId);
        contract.setSignDate(LocalDate.now());
        contract.setStartDate(LocalDate.now());
        contract.setEndDate(LocalDate.now().plusYears(1));
        contract.setPaymentDays(30);
        contract.setDeliveryDays(7);
        contract.setWarrantyMonths(12);
        contract.setServiceMonths(12);
        return createContract(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract updateContract(Long contractId, Contract contract) {
        Contract existing = getById(contractId);
        if (existing == null) {
            throw BusinessException.notFound("合同不存在");
        }
        if (existing.getStatus() != ContractStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的合同可以修改");
        }
        contract.setId(contractId);
        updateById(contract);
        return getById(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract submitForApproval(Long contractId) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        if (contract.getStatus() != ContractStatus.DRAFT.getCode()) {
            throw BusinessException.badRequest("只有草稿状态的合同可以提交审批");
        }
        contract.setStatus(ContractStatus.PENDING_APPROVAL.getCode());
        updateById(contract);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract approve(Long contractId, Long approverId, String note) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        if (contract.getStatus() != ContractStatus.PENDING_APPROVAL.getCode()) {
            throw BusinessException.badRequest("只有待审批状态的合同可以审批");
        }
        contract.setStatus(ContractStatus.APPROVED.getCode());
        contract.setApprovedBy(approverId);
        contract.setApprovedTime(LocalDateTime.now());
        contract.setApprovedNote(note);
        updateById(contract);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract reject(Long contractId, Long rejecterId, String reason) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        if (contract.getStatus() != ContractStatus.PENDING_APPROVAL.getCode()) {
            throw BusinessException.badRequest("只有待审批状态的合同可以拒绝");
        }
        contract.setStatus(ContractStatus.DRAFT.getCode());
        updateById(contract);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract sign(Long contractId, Long signerId, String signMethod, String location) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        if (contract.getStatus() != ContractStatus.APPROVED.getCode() && contract.getStatus() != ContractStatus.PENDING_SIGN.getCode()) {
            throw BusinessException.badRequest("只有已审批或待签署状态的合同可以签署");
        }
        contract.setStatus(ContractStatus.SIGNED.getCode());
        contract.setSignedBy(signerId);
        contract.setSignedTime(LocalDateTime.now());
        contract.setSignedLocation(location);
        updateById(contract);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract makeEffective(Long contractId) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        if (contract.getStatus() != ContractStatus.SIGNED.getCode()) {
            throw BusinessException.badRequest("只有已签署状态的合同可以生效");
        }
        contract.setStatus(ContractStatus.EFFECTIVE.getCode());
        contract.setEffectiveBy(contract.getCreateBy());
        contract.setEffectiveTime(LocalDateTime.now());
        updateById(contract);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract complete(Long contractId) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        contract.setStatus(ContractStatus.COMPLETED.getCode());
        contract.setExecutionProgress(100);
        updateById(contract);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract terminate(Long contractId, Long terminatorId, String reason) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        contract.setStatus(ContractStatus.TERMINATED.getCode());
        contract.setTerminatedBy(terminatorId);
        contract.setTerminatedTime(LocalDateTime.now());
        contract.setTerminatedReason(reason);
        updateById(contract);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract cancel(Long contractId, String reason) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        if (contract.getStatus() == ContractStatus.EFFECTIVE.getCode() || contract.getStatus() == ContractStatus.EXECUTING.getCode()) {
            throw BusinessException.badRequest("生效中的合同不能取消");
        }
        contract.setStatus(ContractStatus.CANCELLED.getCode());
        contract.setRemark(reason);
        updateById(contract);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract renew(Long contractId, LocalDate newEndDate) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        contract.setEndDate(newEndDate);
        contract.setRenewalCount(contract.getRenewalCount() + 1);
        updateById(contract);
        return contract;
    }

    @Override
    public List<Contract> getExpiringContracts(int days) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days);
        return baseMapper.selectExpiringContracts(today, endDate);
    }

    @Override
    public List<Contract> getExpiredContracts() {
        return baseMapper.selectExpiredContracts(LocalDate.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markExpiredContracts() {
        List<Contract> expired = getExpiredContracts();
        for (Contract contract : expired) {
            contract.setStatus(ContractStatus.EXPIRED.getCode());
            updateById(contract);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract updateExecutionProgress(Long contractId, Integer progress, BigDecimal executionAmount) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        contract.setExecutionProgress(progress);
        contract.setExecutionAmount(executionAmount);
        if (progress >= 100) {
            contract.setStatus(ContractStatus.COMPLETED.getCode());
        } else if (progress > 0 && contract.getStatus() == ContractStatus.EFFECTIVE.getCode()) {
            contract.setStatus(ContractStatus.EXECUTING.getCode());
        }
        updateById(contract);
        return contract;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Contract updatePaidAmount(Long contractId, BigDecimal paidAmount) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        contract.setPaidAmount(paidAmount);
        contract.setPendingAmount(contract.getContractAmount().subtract(paidAmount));
        updateById(contract);
        return contract;
    }

    @Override
    public List<ContractClause> getContractClauses(Long contractId) {
        return clauseMapper.selectByContractId(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractClause addClause(Long contractId, ContractClause clause) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        List<ContractClause> existingClauses = getContractClauses(contractId);
        clause.setContractId(contractId);
        clause.setClauseNo(existingClauses.size() + 1);
        clause.setTenantId(contract.getTenantId());
        clauseMapper.insert(clause);
        return clause;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractClause updateClause(Long clauseId, ContractClause clause) {
        ContractClause existing = clauseMapper.selectById(clauseId);
        if (existing == null) {
            throw BusinessException.notFound("合同条款不存在");
        }
        clause.setId(clauseId);
        clauseMapper.updateById(clause);
        return clauseMapper.selectById(clauseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeClause(Long clauseId) {
        clauseMapper.deleteById(clauseId);
    }

    @Override
    public List<ContractAttachment> getAttachments(Long contractId) {
        return attachmentMapper.selectByContractId(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractAttachment addAttachment(Long contractId, ContractAttachment attachment) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        attachment.setContractId(contractId);
        attachment.setTenantId(contract.getTenantId());
        attachment.setUploadedBy(contract.getCreateBy());
        attachment.setUploadedTime(LocalDateTime.now());
        attachmentMapper.insert(attachment);
        return attachment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAttachment(Long attachmentId) {
        attachmentMapper.deleteById(attachmentId);
    }

    @Override
    public List<ContractPayment> getPayments(Long contractId) {
        return paymentMapper.selectByContractId(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractPayment addPayment(Long contractId, ContractPayment payment) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        List<ContractPayment> existingPayments = getPayments(contractId);
        payment.setContractId(contractId);
        payment.setPaymentNo(existingPayments.size() + 1);
        payment.setTenantId(contract.getTenantId());
        payment.setStatus(0);
        paymentMapper.insert(payment);
        return payment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractPayment updatePayment(Long paymentId, ContractPayment payment) {
        ContractPayment existing = paymentMapper.selectById(paymentId);
        if (existing == null) {
            throw BusinessException.notFound("付款计划不存在");
        }
        payment.setId(paymentId);
        paymentMapper.updateById(payment);
        return paymentMapper.selectById(paymentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractPayment confirmPayment(Long paymentId, BigDecimal actualAmount) {
        ContractPayment payment = paymentMapper.selectById(paymentId);
        if (payment == null) {
            throw BusinessException.notFound("付款计划不存在");
        }
        payment.setStatus(2);
        payment.setActualAmount(actualAmount);
        payment.setActualDate(LocalDateTime.now());
        paymentMapper.updateById(payment);
        BigDecimal totalPaid = paymentMapper.sumActualAmountByContractId(payment.getContractId());
        updatePaidAmount(payment.getContractId(), totalPaid);
        return payment;
    }

    @Override
    public List<ContractPayment> getDuePayments(Long contractId) {
        return paymentMapper.selectDuePayments(contractId, LocalDateTime.now());
    }

    @Override
    public List<ContractChange> getChanges(Long contractId) {
        return changeMapper.selectByContractId(contractId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractChange proposeChange(Long contractId, ContractChange change) {
        Contract contract = getById(contractId);
        if (contract == null) {
            throw BusinessException.notFound("合同不存在");
        }
        String changeNo = contract.getContractNo() + "-C" + String.format("%02d", getChanges(contractId).size() + 1);
        change.setContractId(contractId);
        change.setChangeNo(changeNo);
        change.setTenantId(contract.getTenantId());
        change.setStatus(0);
        change.setProposedBy(contract.getCreateBy());
        change.setProposedTime(LocalDateTime.now());
        changeMapper.insert(change);
        return change;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractChange approveChange(Long changeId, Long approverId, String note) {
        ContractChange change = changeMapper.selectById(changeId);
        if (change == null) {
            throw BusinessException.notFound("合同变更不存在");
        }
        change.setStatus(1);
        change.setApprovedBy(approverId);
        change.setApprovedTime(LocalDateTime.now());
        change.setApprovedNote(note);
        changeMapper.updateById(change);
        return change;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractChange rejectChange(Long changeId, Long rejecterId, String reason) {
        ContractChange change = changeMapper.selectById(changeId);
        if (change == null) {
            throw BusinessException.notFound("合同变更不存在");
        }
        change.setStatus(2);
        changeMapper.updateById(change);
        return change;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ContractChange executeChange(Long changeId) {
        ContractChange change = changeMapper.selectById(changeId);
        if (change == null) {
            throw BusinessException.notFound("合同变更不存在");
        }
        if (change.getStatus() != 1) {
            throw BusinessException.badRequest("只有已审批的变更可以执行");
        }
        change.setStatus(3);
        change.setExecutedBy(change.getApprovedBy());
        change.setExecutedTime(LocalDateTime.now());
        changeMapper.updateById(change);
        return change;
    }

    @Override
    public BigDecimal calculatePaidPercentage(Long contractId) {
        Contract contract = getById(contractId);
        if (contract == null || contract.getContractAmount() == null || contract.getContractAmount().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return contract.getPaidAmount().divide(contract.getContractAmount(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    @Override
    public Integer calculateDaysRemaining(Long contractId) {
        Contract contract = getById(contractId);
        if (contract == null || contract.getEndDate() == null) {
            return 0;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());
        return (int) Math.max(days, 0);
    }

    @Override
    public Boolean isExpiring(Long contractId, int days) {
        Contract contract = getById(contractId);
        if (contract == null || contract.getEndDate() == null) {
            return false;
        }
        long remaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());
        return remaining >= 0 && remaining <= days;
    }
}