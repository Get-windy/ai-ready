package cn.aiedge.erp.purchase.service;

import cn.aiedge.erp.purchase.dto.ContractStatisticsDTO;
import cn.aiedge.erp.purchase.dto.PurchaseContractQueryDTO;
import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.entity.PurchaseContractItem;
import cn.aiedge.erp.purchase.entity.PurchaseSupplierQuote;
import cn.aiedge.erp.purchase.enums.ContractStatus;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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

    // ── 以下 4 个方法对应前端合同列表页（菜单 81010）的 CRUD，2026-09-21 补齐 ──
    // 此前该页调用的 /page、POST /、PUT /{id}、DELETE /{id} 在后端**都不存在**
    // （Controller 只有详情/审批类端点，且 purchase_contract 表本身也缺失）⇒ 整页不可用。

    /** 分页查询合同（菜单 81010） */
    Page<PurchaseContract> pageContracts(PurchaseContractQueryDTO query);

    /** 新增合同（状态强制为草稿） */
    PurchaseContract createContract(PurchaseContract contract);

    /** 编辑合同（仅草稿/已驳回可改） */
    PurchaseContract updateContract(Long id, PurchaseContract contract);

    /** 删除合同（仅草稿可删；级联删除明细） */
    void deleteContract(Long id);
}