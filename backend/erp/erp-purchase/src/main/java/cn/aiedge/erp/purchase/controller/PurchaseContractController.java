package cn.aiedge.erp.purchase.controller;

import cn.aiedge.erp.purchase.dto.ContractStatisticsDTO;
import cn.aiedge.erp.purchase.entity.PurchaseContract;
import cn.aiedge.erp.purchase.service.PurchaseContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 采购合同Controller - RESTful API
 */
@RestController
@RequestMapping("/api/erp/purchase/contract")
@RequiredArgsConstructor
public class PurchaseContractController {

    private final PurchaseContractService contractService;

    /**
     * 获取合同详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseContract> getContractById(@PathVariable Long id) {
        PurchaseContract contract = contractService.getContractById(id);
        return ResponseEntity.ok(contract);
    }

    /**
     * 根据合同号查询合同
     */
    @GetMapping("/by-no/{contractNo}")
    public ResponseEntity<PurchaseContract> getContractByNo(@PathVariable String contractNo) {
        PurchaseContract contract = contractService.getContractByNo(contractNo);
        return ResponseEntity.ok(contract);
    }

    /**
     * 根据供应商查询合同
     */
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<List<PurchaseContract>> queryContractsBySupplier(@PathVariable Long supplierId) {
        List<PurchaseContract> contracts = contractService.queryContractsBySupplier(supplierId);
        return ResponseEntity.ok(contracts);
    }

    /**
     * 提交审批
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<PurchaseContract> submitForApproval(
            @PathVariable Long id,
            @RequestParam String reason) {
        PurchaseContract contract = contractService.submitForApproval(id, reason);
        return ResponseEntity.ok(contract);
    }

    /**
     * 审批合同
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<PurchaseContract> approveContract(
            @PathVariable Long id,
            @RequestParam Long approverId,
            @RequestParam String comment,
            @RequestParam boolean approved) {
        PurchaseContract contract = contractService.approveContract(id, approverId, comment, approved);
        return ResponseEntity.ok(contract);
    }

    /**
     * 激活合同
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<PurchaseContract> activateContract(@PathVariable Long id) {
        PurchaseContract contract = contractService.activateContract(id);
        return ResponseEntity.ok(contract);
    }

    /**
     * 终止合同
     */
    @PostMapping("/{id}/terminate")
    public ResponseEntity<PurchaseContract> terminateContract(
            @PathVariable Long id,
            @RequestParam String reason) {
        PurchaseContract contract = contractService.terminateContract(id, reason);
        return ResponseEntity.ok(contract);
    }

    /**
     * 归档合同
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<PurchaseContract> archiveContract(
            @PathVariable Long id,
            @RequestParam String archiveNo,
            @RequestParam String reason) {
        PurchaseContract contract = contractService.archiveContract(id, archiveNo, reason);
        return ResponseEntity.ok(contract);
    }

    /**
     * 获取合同统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<ContractStatisticsDTO> generateContractStatistics() {
        ContractStatisticsDTO statistics = contractService.generateContractStatistics();
        return ResponseEntity.ok(statistics);
    }
}
