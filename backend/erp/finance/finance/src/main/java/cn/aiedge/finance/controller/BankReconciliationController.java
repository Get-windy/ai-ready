package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.BankReconciliationVO;
import cn.aiedge.finance.dto.BankStatementImportDTO;
import cn.aiedge.finance.entity.BankReconciliation;
import cn.aiedge.finance.entity.BankStatement;
import cn.aiedge.finance.enums.BankStatementStatus;
import cn.aiedge.finance.enums.BankTransactionType;
import cn.aiedge.finance.service.BankReconciliationService;
import cn.aiedge.finance.service.BankStatementService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance/bank")
@Tag(name = "银行对账管理", description = "银行流水导入和对账")
public class BankReconciliationController {
    
    @Autowired
    private BankStatementService bankStatementService;
    
    @Autowired
    private BankReconciliationService bankReconciliationService;
    
    @GetMapping("/statement/page")
    @Operation(summary = "分页查询银行流水")
    public Page<BankStatement> pageStatements(
            @Parameter(description = "账户ID") @RequestParam Long accountId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<BankStatement> page = new Page<>(pageNum, pageSize);
        return bankStatementService.pageList(tenantId, accountId, status, startDate, endDate, page);
    }
    
    @GetMapping("/statement/unmatched/{accountId}")
    @Operation(summary = "获取未对账流水")
    public List<BankStatement> listUnmatched(@PathVariable Long accountId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankStatementService.listUnmatched(tenantId, accountId);
    }
    
    @PostMapping("/statement/import")
    @Operation(summary = "导入银行流水")
    public boolean importStatements(@RequestBody List<BankStatementImportDTO> dtos) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<BankStatement> statements = dtos.stream().map(dto -> {
            BankStatement statement = new BankStatement();
            BeanUtils.copyProperties(dto, statement);
            return statement;
        }).collect(Collectors.toList());
        Long accountId = dtos.get(0).getAccountId();
        return bankStatementService.importStatements(tenantId, accountId, statements);
    }
    
    @PutMapping("/statement/{id}/match")
    @Operation(summary = "手工对账")
    public boolean manualMatch(@PathVariable Long id, @RequestParam Long transactionId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankStatementService.manualMatch(tenantId, id, transactionId);
    }
    
    @PutMapping("/statement/auto-match/{accountId}")
    @Operation(summary = "自动对账")
    public boolean autoMatch(@PathVariable Long accountId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankStatementService.autoMatch(tenantId, accountId);
    }
    
    @PutMapping("/statement/{id}/unmatch")
    @Operation(summary = "取消对账")
    public boolean unmatch(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankStatementService.unmatch(tenantId, id);
    }
    
    @DeleteMapping("/statement/{id}")
    @Operation(summary = "删除银行流水")
    public boolean deleteStatement(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankStatementService.deleteStatement(tenantId, id);
    }
    
    @GetMapping("/statement/summary/{accountId}")
    @Operation(summary = "获取账户流水汇总")
    public Map<String, BigDecimal> getAccountSummary(@PathVariable Long accountId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankStatementService.getAccountSummary(tenantId, accountId);
    }
    
    @GetMapping("/statement/count-unmatched/{accountId}")
    @Operation(summary = "统计未对账流水数量")
    public Integer countUnmatched(@PathVariable Long accountId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankStatementService.countUnmatched(tenantId, accountId);
    }
    
    @GetMapping("/reconciliation/page")
    @Operation(summary = "分页查询对账记录")
    public Page<BankReconciliationVO> pageReconciliation(
            @Parameter(description = "账户ID") @RequestParam(required = false) Long accountId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<BankReconciliation> page = new Page<>(pageNum, pageSize);
        Page<BankReconciliation> result = bankReconciliationService.pageList(tenantId, accountId, status, page);
        Page<BankReconciliationVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }
    
    @GetMapping("/reconciliation/{id}")
    @Operation(summary = "获取对账详情")
    public Map<String, Object> getReconciliationDetail(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankReconciliationService.getReconciliationDetail(tenantId, id);
    }
    
    @PostMapping("/reconciliation")
    @Operation(summary = "创建对账记录")
    public BankReconciliation createReconciliation(@RequestParam Long accountId, @RequestParam String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankReconciliationService.createReconciliation(tenantId, accountId, period);
    }
    
    @PutMapping("/reconciliation/{id}/calculate")
    @Operation(summary = "计算对账差异")
    public boolean calculateReconciliation(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankReconciliationService.calculateReconciliation(tenantId, id);
    }
    
    @PutMapping("/reconciliation/{id}/approve")
    @Operation(summary = "审核对账")
    public boolean approveReconciliation(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankReconciliationService.approve(tenantId, id);
    }
    
    @DeleteMapping("/reconciliation/{id}")
    @Operation(summary = "删除对账记录")
    public boolean deleteReconciliation(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return bankReconciliationService.deleteReconciliation(tenantId, id);
    }
    
    @GetMapping("/transaction-types")
    @Operation(summary = "获取交易类型列表")
    public List<BankTransactionType> listTransactionTypes() {
        return List.of(BankTransactionType.values());
    }
    
    @GetMapping("/statement-statuses")
    @Operation(summary = "获取流水状态列表")
    public List<BankStatementStatus> listStatementStatuses() {
        return List.of(BankStatementStatus.values());
    }
    
    private BankReconciliationVO convertToVO(BankReconciliation reconciliation) {
        BankReconciliationVO vo = new BankReconciliationVO();
        BeanUtils.copyProperties(reconciliation, vo);
        vo.setIsBalanced(reconciliation.getDifference().compareTo(BigDecimal.ZERO) == 0);
        return vo;
    }
}