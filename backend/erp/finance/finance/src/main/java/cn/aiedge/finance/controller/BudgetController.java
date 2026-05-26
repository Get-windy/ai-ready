package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.BudgetCreateDTO;
import cn.aiedge.finance.entity.Budget;
import cn.aiedge.finance.entity.BudgetAdjustment;
import cn.aiedge.finance.entity.BudgetExecution;
import cn.aiedge.finance.entity.BudgetItem;
import cn.aiedge.finance.enums.BudgetStatus;
import cn.aiedge.finance.enums.BudgetType;
import cn.aiedge.finance.service.BudgetAdjustmentService;
import cn.aiedge.finance.service.BudgetExecutionMapper;
import cn.aiedge.finance.service.BudgetItemService;
import cn.aiedge.finance.service.BudgetService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance/budget")
@Tag(name = "预算管理", description = "预算编制、执行监控和调整")
public class BudgetController {
    
    @Autowired
    private BudgetService budgetService;
    
    @Autowired
    private BudgetItemService budgetItemService;
    
    @Autowired
    private BudgetAdjustmentService budgetAdjustmentService;
    
    @Autowired
    private BudgetExecutionMapper budgetExecutionMapper;
    
    @GetMapping("/page")
    @Operation(summary = "分页查询预算")
    public Page<Budget> page(
            @Parameter(description = "预算编码") @RequestParam(required = false) String budgetCode,
            @Parameter(description = "预算名称") @RequestParam(required = false) String budgetName,
            @Parameter(description = "预算类型") @RequestParam(required = false) Integer budgetType,
            @Parameter(description = "期间") @RequestParam(required = false) String period,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<Budget> page = new Page<>(pageNum, pageSize);
        return budgetService.pageList(tenantId, budgetCode, budgetName, budgetType, period, status, page);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取预算详情")
    public Budget getById(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.getDetail(tenantId, id);
    }
    
    @PostMapping
    @Operation(summary = "创建预算")
    public boolean create(@Valid @RequestBody BudgetCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Budget budget = new Budget();
        BeanUtils.copyProperties(dto, budget);
        budget.setTenantId(tenantId);
        budget.setBudgetCode("BG" + dto.getPeriod() + System.currentTimeMillis());
        List<BudgetItem> items = new ArrayList<>();
        if (dto.getItems() != null) {
            for (BudgetCreateDTO.BudgetItemDTO itemDTO : dto.getItems()) {
                BudgetItem item = new BudgetItem();
                BeanUtils.copyProperties(itemDTO, item);
                item.setTenantId(tenantId);
                items.add(item);
            }
        }
        budget.setItems(items);
        return budgetService.createBudget(budget);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新预算")
    public boolean update(@PathVariable Long id, @Valid @RequestBody BudgetCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Budget budget = budgetService.getById(id);
        if (budget == null) {
            throw new RuntimeException("预算不存在");
        }
        BeanUtils.copyProperties(dto, budget);
        return budgetService.updateBudget(budget);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除预算")
    public boolean delete(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.deleteBudget(tenantId, id);
    }
    
    @PutMapping("/{id}/submit")
    @Operation(summary = "提交审批")
    public boolean submit(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.submitForApproval(tenantId, id);
    }
    
    @PutMapping("/{id}/approve")
    @Operation(summary = "审批通过")
    public boolean approve(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.approve(tenantId, id);
    }
    
    @PutMapping("/{id}/reject")
    @Operation(summary = "审批驳回")
    public boolean reject(@PathVariable Long id, @RequestParam String reason) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.reject(tenantId, id, reason);
    }
    
    @PutMapping("/{id}/start")
    @Operation(summary = "开始执行")
    public boolean start(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.startExecution(tenantId, id);
    }
    
    @PutMapping("/{id}/close")
    @Operation(summary = "关闭预算")
    public boolean close(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.close(tenantId, id);
    }
    
    @GetMapping("/summary/{period}")
    @Operation(summary = "获取预算汇总")
    public Map<String, Object> getSummary(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.getBudgetSummary(tenantId, period);
    }
    
    @GetMapping("/item/alert")
    @Operation(summary = "获取预警预算明细")
    public List<BudgetItem> listAlertItems() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetItemService.listAlertItems(tenantId);
    }
    
    @PostMapping("/execution")
    @Operation(summary = "记录预算执行")
    public boolean recordExecution(
            @RequestParam Long budgetItemId,
            @RequestParam BigDecimal amount,
            @RequestParam String sourceType,
            @RequestParam Long sourceId,
            @RequestParam(required = false) String sourceNo) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.recordExecution(tenantId, budgetItemId, amount, sourceType, sourceId, sourceNo);
    }
    
    @GetMapping("/execution/{budgetItemId}")
    @Operation(summary = "获取预算执行记录")
    public List<BudgetExecution> listExecutions(@PathVariable Long budgetItemId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetExecutionMapper.listByBudgetItemId(tenantId, budgetItemId);
    }
    
    @GetMapping("/adjustment/page")
    @Operation(summary = "分页查询预算调整")
    public Page<BudgetAdjustment> pageAdjustment(
            @Parameter(description = "预算ID") @RequestParam(required = false) Long budgetId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<BudgetAdjustment> page = new Page<>(pageNum, pageSize);
        return budgetAdjustmentService.pageList(tenantId, budgetId, status, page);
    }
    
    @PostMapping("/adjustment")
    @Operation(summary = "创建预算调整")
    public boolean createAdjustment(@RequestBody BudgetAdjustment adjustment) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        adjustment.setTenantId(tenantId);
        return budgetAdjustmentService.createAdjustment(adjustment);
    }
    
    @PutMapping("/adjustment/{id}/submit")
    @Operation(summary = "提交调整审批")
    public boolean submitAdjustment(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetAdjustmentService.submitForApproval(tenantId, id);
    }
    
    @PutMapping("/adjustment/{id}/approve")
    @Operation(summary = "审批调整")
    public boolean approveAdjustment(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetAdjustmentService.approve(tenantId, id);
    }
    
    @PutMapping("/adjustment/{id}/reject")
    @Operation(summary = "驳回调整")
    public boolean rejectAdjustment(@PathVariable Long id, @RequestParam String reason) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetAdjustmentService.reject(tenantId, id, reason);
    }
    
    @PostMapping("/generate/{year}")
    @Operation(summary = "从上年生成预算")
    public List<Budget> generateFromLastYear(@PathVariable Integer year, @RequestParam BigDecimal adjustRate) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return budgetService.generateFromLastYear(tenantId, year, adjustRate);
    }
    
    @GetMapping("/types")
    @Operation(summary = "获取预算类型列表")
    public List<BudgetType> listTypes() {
        return List.of(BudgetType.values());
    }
    
    @GetMapping("/statuses")
    @Operation(summary = "获取预算状态列表")
    public List<BudgetStatus> listStatuses() {
        return List.of(BudgetStatus.values());
    }
}