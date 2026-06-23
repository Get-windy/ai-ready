package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.ContractStatisticsDTO;
import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.entity.PurchaseContractItem;
import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.enums.ContractStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * 采购合同Service接口
 */
public interface PurchaseContractService {

    PurchaseContract generateContractFromQuote(PurchaseSupplierQuote quote, List<PurchaseContractItem> items);

    PurchaseContract submitForApproval(Long id, String reason);

    PurchaseContract approveContract(Long id, Long approverId, String comment, boolean approved);

    PurchaseContract activateContract(Long id);

    PurchaseContract updateExecutionProgress(Long id, BigDecimal executedAmount, BigDecimal executedPercent);

    PurchaseContract completeContract(Long id);

    PurchaseContract terminateContract(Long id, String reason);

    PurchaseContract archiveContract(Long id, String archiveNo, String reason);

    PurchaseContract getContractById(Long id);

    PurchaseContract getContractByNo(String contractNo);

    List<PurchaseContract> queryContractsBySupplier(Long supplierId);

    List<PurchaseContract> queryContractsByStatus(ContractStatus status);

    List<PurchaseContract> getContractsByInquiry(Long inquiryId);

    List<PurchaseContractItem> getContractItems(Long contractId);

    String generateContractNo();

    BigDecimal calculateContractAmount(List<PurchaseContractItem> items);

    boolean validateContractTerms(Long contractId);

    List<PurchaseContract> queryExpiringContracts(int days);

    PurchaseContract renewContract(Long id, int extendMonths, String reason);

    PurchaseContract modifyContract(Long id, String modificationReason, String detail);

    ContractStatisticsDTO generateContractStatistics();
}