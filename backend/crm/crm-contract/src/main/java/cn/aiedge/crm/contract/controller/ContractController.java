package cn.aiedge.crm.contract.controller;

import cn.aiedge.crm.contract.dto.ContractCreateDTO;
import cn.aiedge.crm.contract.dto.ContractVO;
import cn.aiedge.crm.contract.entity.Contract;
import cn.aiedge.crm.contract.entity.ContractAttachment;
import cn.aiedge.crm.contract.entity.ContractChange;
import cn.aiedge.crm.contract.entity.ContractClause;
import cn.aiedge.crm.contract.entity.ContractPayment;
import cn.aiedge.crm.contract.enums.ContractStatus;
import cn.aiedge.crm.contract.service.ContractService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/crm/contract")
@RequiredArgsConstructor
@Tag(name = "CRM合同管理", description = "合同创建、审批、签署、执行跟踪、变更管理等操作")
public class ContractController {

    private final ContractService contractService;

    @GetMapping("/page")
    @Operation(summary = "分页查询合同")
    public Page<ContractVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "商机ID") @RequestParam(required = false) Long opportunityId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "合同类型") @RequestParam(required = false) Integer contractType,
            @Parameter(description = "销售员ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<Contract> page = contractService.pageList(keyword, customerId, opportunityId, status, contractType, salesPersonId, pageNum, pageSize);
        Page<ContractVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取合同详情")
    public ContractVO getById(@PathVariable Long id) {
        Contract contract = contractService.getById(id);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        ContractVO vo = convertToVO(contract);
        vo.setPaidPercentage(contractService.calculatePaidPercentage(id));
        vo.setDaysRemaining(contractService.calculateDaysRemaining(id));
        vo.setIsExpiring(contractService.isExpiring(id, 30));
        return vo;
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "获取客户的合同列表")
    public List<ContractVO> listByCustomerId(@PathVariable Long customerId) {
        return contractService.listByCustomerId(customerId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/opportunity/{opportunityId}")
    @Operation(summary = "获取商机的合同列表")
    public List<ContractVO> listByOpportunityId(@PathVariable Long opportunityId) {
        return contractService.listByOpportunityId(opportunityId).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/expiring")
    @Operation(summary = "获取即将到期合同")
    public List<ContractVO> getExpiringContracts(@RequestParam(defaultValue = "30") int days) {
        return contractService.getExpiringContracts(days).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/expired")
    @Operation(summary = "获取已过期合同")
    public List<ContractVO> getExpiredContracts() {
        return contractService.getExpiredContracts().stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "创建合同")
    public ContractVO create(@RequestBody ContractCreateDTO dto) {
        Contract contract = new Contract();
        BeanUtils.copyProperties(dto, contract);
        contract.setTenantId(1L);
        contract.setCreateBy(StpUtil.getLoginIdAsLong());
        Contract created = contractService.createContract(contract);
        return convertToVO(created);
    }

    @PostMapping("/from-quotation/{quotationId}")
    @Operation(summary = "从报价单创建合同")
    public ContractVO createFromQuotation(@PathVariable Long quotationId) {
        Contract contract = contractService.createFromQuotation(quotationId);
        return convertToVO(contract);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新合同")
    public ContractVO update(@PathVariable Long id, @RequestBody ContractCreateDTO dto) {
        Contract contract = new Contract();
        BeanUtils.copyProperties(dto, contract);
        Contract updated = contractService.updateContract(id, contract);
        return convertToVO(updated);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public ContractVO submitForApproval(@PathVariable Long id) {
        Contract contract = contractService.submitForApproval(id);
        return convertToVO(contract);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public ContractVO approve(@PathVariable Long id, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        Contract contract = contractService.approve(id, approverId, note);
        return convertToVO(contract);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "审批拒绝")
    public ContractVO reject(@PathVariable Long id, @RequestParam String reason) {
        Long rejecterId = StpUtil.getLoginIdAsLong();
        Contract contract = contractService.reject(id, rejecterId, reason);
        return convertToVO(contract);
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "签署合同")
    public ContractVO sign(
            @PathVariable Long id,
            @RequestParam String signMethod,
            @RequestParam(required = false) String location) {
        Long signerId = StpUtil.getLoginIdAsLong();
        Contract contract = contractService.sign(id, signerId, signMethod, location);
        return convertToVO(contract);
    }

    @PostMapping("/{id}/effective")
    @Operation(summary = "合同生效")
    public ContractVO makeEffective(@PathVariable Long id) {
        Contract contract = contractService.makeEffective(id);
        return convertToVO(contract);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "合同完成")
    public ContractVO complete(@PathVariable Long id) {
        Contract contract = contractService.complete(id);
        return convertToVO(contract);
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "终止合同")
    public ContractVO terminate(@PathVariable Long id, @RequestParam String reason) {
        Long terminatorId = StpUtil.getLoginIdAsLong();
        Contract contract = contractService.terminate(id, terminatorId, reason);
        return convertToVO(contract);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消合同")
    public ContractVO cancel(@PathVariable Long id, @RequestParam String reason) {
        Contract contract = contractService.cancel(id, reason);
        return convertToVO(contract);
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "续签合同")
    public ContractVO renew(@PathVariable Long id, @RequestParam LocalDate newEndDate) {
        Contract contract = contractService.renew(id, newEndDate);
        return convertToVO(contract);
    }

    @PostMapping("/mark-expired")
    @Operation(summary = "标记过期合同")
    public void markExpiredContracts() {
        contractService.markExpiredContracts();
    }

    @PutMapping("/{id}/progress")
    @Operation(summary = "更新执行进度")
    public ContractVO updateProgress(
            @PathVariable Long id,
            @RequestParam Integer progress,
            @RequestParam BigDecimal executionAmount) {
        Contract contract = contractService.updateExecutionProgress(id, progress, executionAmount);
        return convertToVO(contract);
    }

    @GetMapping("/{id}/clauses")
    @Operation(summary = "获取合同条款")
    public List<ContractClause> getContractClauses(@PathVariable Long id) {
        return contractService.getContractClauses(id);
    }

    @PostMapping("/{id}/clauses")
    @Operation(summary = "添加合同条款")
    public ContractClause addClause(@PathVariable Long id, @RequestBody ContractClause clause) {
        return contractService.addClause(id, clause);
    }

    @PutMapping("/{id}/clauses/{clauseId}")
    @Operation(summary = "更新合同条款")
    public ContractClause updateClause(@PathVariable Long clauseId, @RequestBody ContractClause clause) {
        return contractService.updateClause(clauseId, clause);
    }

    @DeleteMapping("/{id}/clauses/{clauseId}")
    @Operation(summary = "删除合同条款")
    public void removeClause(@PathVariable Long clauseId) {
        contractService.removeClause(clauseId);
    }

    @GetMapping("/{id}/attachments")
    @Operation(summary = "获取合同附件")
    public List<ContractAttachment> getAttachments(@PathVariable Long id) {
        return contractService.getAttachments(id);
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "添加合同附件")
    public ContractAttachment addAttachment(@PathVariable Long id, @RequestBody ContractAttachment attachment) {
        return contractService.addAttachment(id, attachment);
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @Operation(summary = "删除合同附件")
    public void removeAttachment(@PathVariable Long attachmentId) {
        contractService.removeAttachment(attachmentId);
    }

    @GetMapping("/{id}/payments")
    @Operation(summary = "获取付款计划")
    public List<ContractPayment> getPayments(@PathVariable Long id) {
        return contractService.getPayments(id);
    }

    @GetMapping("/{id}/payments/due")
    @Operation(summary = "获取到期付款")
    public List<ContractPayment> getDuePayments(@PathVariable Long id) {
        return contractService.getDuePayments(id);
    }

    @PostMapping("/{id}/payments")
    @Operation(summary = "添加付款计划")
    public ContractPayment addPayment(@PathVariable Long id, @RequestBody ContractPayment payment) {
        return contractService.addPayment(id, payment);
    }

    @PutMapping("/{id}/payments/{paymentId}")
    @Operation(summary = "更新付款计划")
    public ContractPayment updatePayment(@PathVariable Long paymentId, @RequestBody ContractPayment payment) {
        return contractService.updatePayment(paymentId, payment);
    }

    @PostMapping("/{id}/payments/{paymentId}/confirm")
    @Operation(summary = "确认付款")
    public ContractPayment confirmPayment(@PathVariable Long paymentId, @RequestParam BigDecimal actualAmount) {
        return contractService.confirmPayment(paymentId, actualAmount);
    }

    @GetMapping("/{id}/changes")
    @Operation(summary = "获取合同变更记录")
    public List<ContractChange> getChanges(@PathVariable Long id) {
        return contractService.getChanges(id);
    }

    @PostMapping("/{id}/changes")
    @Operation(summary = "提出合同变更")
    public ContractChange proposeChange(@PathVariable Long id, @RequestBody ContractChange change) {
        return contractService.proposeChange(id, change);
    }

    @PostMapping("/{id}/changes/{changeId}/approve")
    @Operation(summary = "审批变更")
    public ContractChange approveChange(@PathVariable Long changeId, @RequestParam(required = false) String note) {
        Long approverId = StpUtil.getLoginIdAsLong();
        return contractService.approveChange(changeId, approverId, note);
    }

    @PostMapping("/{id}/changes/{changeId}/reject")
    @Operation(summary = "拒绝变更")
    public ContractChange rejectChange(@PathVariable Long changeId, @RequestParam String reason) {
        Long rejecterId = StpUtil.getLoginIdAsLong();
        return contractService.rejectChange(changeId, rejecterId, reason);
    }

    @PostMapping("/{id}/changes/{changeId}/execute")
    @Operation(summary = "执行变更")
    public ContractChange executeChange(@PathVariable Long changeId) {
        return contractService.executeChange(changeId);
    }

    @GetMapping("/statistics")
    @Operation(summary = "合同统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        for (ContractStatus status : ContractStatus.values()) {
            stats.put(status.getDesc(), contractService.lambdaQuery()
                    .eq(Contract::getStatus, status.getCode())
                    .eq(Contract::getDeleted, 0)
                    .count());
        }
        stats.put("totalContractAmount", contractService.baseMapper.sumEffectiveContractAmount(1L));
        return stats;
    }

    private ContractVO convertToVO(Contract contract) {
        ContractVO vo = new ContractVO();
        BeanUtils.copyProperties(contract, vo);
        for (ContractStatus status : ContractStatus.values()) {
            if (status.getCode().equals(contract.getStatus())) {
                vo.setStatusDesc(status.getDesc());
                break;
            }
        }
        return vo;
    }
}