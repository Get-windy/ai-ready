package cn.aiedge.crm.contract.service;

import cn.aiedge.crm.contract.entity.Contract;
import cn.aiedge.crm.contract.entity.ContractAttachment;
import cn.aiedge.crm.contract.entity.ContractChange;
import cn.aiedge.crm.contract.entity.ContractClause;
import cn.aiedge.crm.contract.entity.ContractPayment;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ContractService extends IService<Contract> {

    Contract getByContractNo(String contractNo);

    Page<Contract> pageList(String keyword, Long customerId, Long opportunityId, Integer status, Integer contractType, Long salesPersonId, int pageNum, int pageSize);

    List<Contract> listByCustomerId(Long customerId);

    List<Contract> listByOpportunityId(Long opportunityId);

    List<Contract> listBySalesPersonId(Long salesPersonId);

    List<Contract> listByStatus(Integer status);

    String generateContractNo();

    List<Contract> exportList(String keyword, Long customerId, Long opportunityId, Integer status, Integer contractType, Long salesPersonId);

    Contract createContract(Contract contract);

    Contract createFromQuotation(Long quotationId);

    Contract updateContract(Long contractId, Contract contract);

    Contract submitForApproval(Long contractId);

    Contract approve(Long contractId, Long approverId, String note);

    Contract reject(Long contractId, Long rejecterId, String reason);

    Contract sign(Long contractId, Long signerId, String signMethod, String location);

    Contract makeEffective(Long contractId);

    Contract complete(Long contractId);

    Contract terminate(Long contractId, Long terminatorId, String reason);

    Contract cancel(Long contractId, String reason);

    Contract renew(Long contractId, LocalDate newEndDate);

    List<Contract> getExpiringContracts(int days);

    List<Contract> getExpiredContracts();

    void markExpiredContracts();

    Contract updateExecutionProgress(Long contractId, Integer progress, BigDecimal executionAmount);

    Contract updatePaidAmount(Long contractId, BigDecimal paidAmount);

    List<ContractClause> getContractClauses(Long contractId);

    ContractClause addClause(Long contractId, ContractClause clause);

    ContractClause updateClause(Long clauseId, ContractClause clause);

    void removeClause(Long clauseId);

    List<ContractAttachment> getAttachments(Long contractId);

    ContractAttachment addAttachment(Long contractId, ContractAttachment attachment);

    void removeAttachment(Long attachmentId);

    List<ContractPayment> getPayments(Long contractId);

    ContractPayment addPayment(Long contractId, ContractPayment payment);

    ContractPayment updatePayment(Long paymentId, ContractPayment payment);

    ContractPayment confirmPayment(Long paymentId, BigDecimal actualAmount);

    List<ContractPayment> getDuePayments(Long contractId);

    List<ContractChange> getChanges(Long contractId);

    ContractChange proposeChange(Long contractId, ContractChange change);

    ContractChange approveChange(Long changeId, Long approverId, String note);

    ContractChange rejectChange(Long changeId, Long rejecterId, String reason);

    ContractChange executeChange(Long changeId);

    BigDecimal calculatePaidPercentage(Long contractId);

    Integer calculateDaysRemaining(Long contractId);

    Boolean isExpiring(Long contractId, int days);
}