package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.AccountBalanceVO;
import cn.aiedge.finance.dto.TrialBalanceVO;
import cn.aiedge.finance.entity.AccountBalance;
import cn.aiedge.finance.entity.AccountSubject;
import cn.aiedge.finance.mapper.AccountSubjectMapper;
import cn.aiedge.finance.service.AccountBalanceService;
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
@RequestMapping("/api/finance/account-balance")
@Tag(name = "科目余额管理", description = "科目余额查询和财务报表")
public class AccountBalanceController {
    
    @Autowired
    private AccountBalanceService accountBalanceService;
    
    @Autowired
    private AccountSubjectMapper accountSubjectMapper;
    
    @GetMapping("/list/{period}")
    @Operation(summary = "获取期间科目余额列表")
    public List<AccountBalanceVO> listByPeriod(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        List<AccountBalance> balances = accountBalanceService.listByPeriod(tenantId, period);
        return balances.stream().map(this::convertToVO).collect(Collectors.toList());
    }
    
    @GetMapping("/page/{period}")
    @Operation(summary = "分页查询科目余额")
    public Page<AccountBalanceVO> page(
            @PathVariable String period,
            @Parameter(description = "科目编码") @RequestParam(required = false) String subjectCode,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<AccountBalance> page = new Page<>(pageNum, pageSize);
        Page<AccountBalance> result = accountBalanceService.pageList(tenantId, period, subjectCode, null, page);
        Page<AccountBalanceVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }
    
    @GetMapping("/trial-balance/{period}")
    @Operation(summary = "获取试算平衡表")
    public TrialBalanceVO getTrialBalance(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Map<String, BigDecimal> data = accountBalanceService.getTrialBalance(tenantId, period);
        TrialBalanceVO vo = new TrialBalanceVO();
        vo.setTotalInitialDebit(data.get("totalInitialDebit"));
        vo.setTotalInitialCredit(data.get("totalInitialCredit"));
        vo.setTotalPeriodDebit(data.get("totalPeriodDebit"));
        vo.setTotalPeriodCredit(data.get("totalPeriodCredit"));
        vo.setTotalEndingDebit(data.get("totalEndingDebit"));
        vo.setTotalEndingCredit(data.get("totalEndingCredit"));
        BigDecimal initialDiff = data.get("totalInitialDebit").subtract(data.get("totalInitialCredit"));
        BigDecimal periodDiff = data.get("totalPeriodDebit").subtract(data.get("totalPeriodCredit"));
        BigDecimal endingDiff = data.get("totalEndingDebit").subtract(data.get("totalEndingCredit"));
        vo.setInitialBalanceDiff(initialDiff);
        vo.setPeriodBalanceDiff(periodDiff);
        vo.setEndingBalanceDiff(endingDiff);
        vo.setIsBalanced(initialDiff.compareTo(BigDecimal.ZERO) == 0 
                && periodDiff.compareTo(BigDecimal.ZERO) == 0 
                && endingDiff.compareTo(BigDecimal.ZERO) == 0);
        vo.setDetails(data);
        return vo;
    }
    
    @GetMapping("/balance-sheet/{period}")
    @Operation(summary = "获取资产负债表")
    public Map<String, Object> getBalanceSheet(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountBalanceService.getBalanceSheet(tenantId, period);
    }
    
    @GetMapping("/income-statement/{period}")
    @Operation(summary = "获取利润表")
    public Map<String, Object> getIncomeStatement(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return accountBalanceService.getIncomeStatement(tenantId, period);
    }
    
    private AccountBalanceVO convertToVO(AccountBalance balance) {
        AccountBalanceVO vo = new AccountBalanceVO();
        BeanUtils.copyProperties(balance, vo);
        AccountSubject subject = accountSubjectMapper.selectById(balance.getSubjectId());
        if (subject != null) {
            vo.setSubjectName(subject.getSubjectName());
            vo.setBalanceDirection(subject.getBalanceDirection());
            BigDecimal endingBalance;
            if (subject.getBalanceDirection() == 1) {
                endingBalance = balance.getEndingDebit().subtract(balance.getEndingCredit());
            } else {
                endingBalance = balance.getEndingCredit().subtract(balance.getEndingDebit());
            }
            vo.setEndingBalance(endingBalance);
        }
        return vo;
    }
}