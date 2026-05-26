package cn.aiedge.finance.controller;

import cn.aiedge.finance.dto.CostCenterCreateDTO;
import cn.aiedge.finance.dto.CostItemCreateDTO;
import cn.aiedge.finance.entity.CostAllocation;
import cn.aiedge.finance.entity.CostAllocationRule;
import cn.aiedge.finance.entity.CostCenter;
import cn.aiedge.finance.entity.CostItem;
import cn.aiedge.finance.enums.CostAllocationMethod;
import cn.aiedge.finance.enums.CostCenterType;
import cn.aiedge.finance.enums.CostType;
import cn.aiedge.finance.service.CostAllocationRuleService;
import cn.aiedge.finance.service.CostAllocationService;
import cn.aiedge.finance.service.CostCenterService;
import cn.aiedge.finance.service.CostItemService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/finance/cost")
@Tag(name = "成本核算管理", description = "成本中心、成本项目和成本分配")
public class CostAccountingController {
    
    @Autowired
    private CostCenterService costCenterService;
    
    @Autowired
    private CostItemService costItemService;
    
    @Autowired
    private CostAllocationService costAllocationService;
    
    @Autowired
    private CostAllocationRuleService costAllocationRuleService;
    
    @GetMapping("/center/tree")
    @Operation(summary = "获取成本中心树")
    public List<CostCenter> getCenterTree() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costCenterService.buildTree(tenantId);
    }
    
    @GetMapping("/center/page")
    @Operation(summary = "分页查询成本中心")
    public Page<CostCenter> pageCenter(
            @Parameter(description = "编码") @RequestParam(required = false) String centerCode,
            @Parameter(description = "名称") @RequestParam(required = false) String centerName,
            @Parameter(description = "类型") @RequestParam(required = false) Integer centerType,
            @Parameter(description = "启用状态") @RequestParam(required = false) Integer enabled,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<CostCenter> page = new Page<>(pageNum, pageSize);
        return costCenterService.pageList(tenantId, centerCode, centerName, centerType, enabled, page);
    }
    
    @PostMapping("/center")
    @Operation(summary = "创建成本中心")
    public boolean createCenter(@Valid @RequestBody CostCenterCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        CostCenter center = new CostCenter();
        BeanUtils.copyProperties(dto, center);
        center.setTenantId(tenantId);
        return costCenterService.createCenter(center);
    }
    
    @PutMapping("/center/{id}")
    @Operation(summary = "更新成本中心")
    public boolean updateCenter(@PathVariable Long id, @Valid @RequestBody CostCenterCreateDTO dto) {
        CostCenter center = costCenterService.getById(id);
        if (center == null) {
            throw new RuntimeException("成本中心不存在");
        }
        BeanUtils.copyProperties(dto, center);
        return costCenterService.updateCenter(center);
    }
    
    @DeleteMapping("/center/{id}")
    @Operation(summary = "删除成本中心")
    public boolean deleteCenter(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costCenterService.deleteCenter(tenantId, id);
    }
    
    @GetMapping("/item/page")
    @Operation(summary = "分页查询成本项目")
    public Page<CostItem> pageItem(
            @Parameter(description = "期间") @RequestParam(required = false) String period,
            @Parameter(description = "成本中心ID") @RequestParam(required = false) Long costCenterId,
            @Parameter(description = "成本类型") @RequestParam(required = false) Integer costType,
            @Parameter(description = "分配状态") @RequestParam(required = false) Integer allocationStatus,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<CostItem> page = new Page<>(pageNum, pageSize);
        return costItemService.pageList(tenantId, period, costCenterId, costType, allocationStatus, page);
    }
    
    @PostMapping("/item")
    @Operation(summary = "创建成本项目")
    public boolean createItem(@Valid @RequestBody CostItemCreateDTO dto) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        CostItem item = new CostItem();
        BeanUtils.copyProperties(dto, item);
        item.setTenantId(tenantId);
        CostCenter center = costCenterService.getById(dto.getCostCenterId());
        if (center != null) {
            item.setCostCenterCode(center.getCenterCode());
            item.setCostCenterName(center.getCenterName());
        }
        return costItemService.createItem(item);
    }
    
    @PutMapping("/item/{id}")
    @Operation(summary = "更新成本项目")
    public boolean updateItem(@PathVariable Long id, @Valid @RequestBody CostItemCreateDTO dto) {
        CostItem item = costItemService.getById(id);
        if (item == null) {
            throw new RuntimeException("成本项目不存在");
        }
        BeanUtils.copyProperties(dto, item);
        return costItemService.updateItem(item);
    }
    
    @DeleteMapping("/item/{id}")
    @Operation(summary = "删除成本项目")
    public boolean deleteItem(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costItemService.deleteItem(tenantId, id);
    }
    
    @PostMapping("/item/{id}/allocate")
    @Operation(summary = "分配成本")
    public boolean allocateItem(@PathVariable Long id,
            @RequestParam Long toCenterId,
            @RequestParam BigDecimal amount) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costItemService.allocate(tenantId, id, toCenterId, amount);
    }
    
    @PostMapping("/item/batch-allocate/{period}")
    @Operation(summary = "批量分配成本")
    public boolean batchAllocate(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costItemService.batchAllocate(tenantId, period);
    }
    
    @GetMapping("/item/summary/{period}")
    @Operation(summary = "获取成本汇总")
    public Map<String, BigDecimal> getCostSummary(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costItemService.getCostSummary(tenantId, period);
    }
    
    @GetMapping("/allocation/page")
    @Operation(summary = "分页查询成本分配")
    public Page<CostAllocation> pageAllocation(
            @Parameter(description = "期间") @RequestParam(required = false) String period,
            @Parameter(description = "来源中心ID") @RequestParam(required = false) Long fromCenterId,
            @Parameter(description = "目标中心ID") @RequestParam(required = false) Long toCenterId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<CostAllocation> page = new Page<>(pageNum, pageSize);
        return costAllocationService.pageList(tenantId, period, fromCenterId, toCenterId, status, page);
    }
    
    @PostMapping("/allocation/{id}/execute")
    @Operation(summary = "执行分配")
    public boolean executeAllocation(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costAllocationService.executeAllocation(tenantId, id);
    }
    
    @PostMapping("/allocation/batch-execute/{period}")
    @Operation(summary = "批量执行分配")
    public boolean batchExecute(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costAllocationService.batchExecute(tenantId, period);
    }
    
    @PostMapping("/allocation/{id}/post")
    @Operation(summary = "记账分配")
    public boolean postAllocation(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costAllocationService.post(tenantId, id);
    }
    
    @PostMapping("/allocation/batch-post/{period}")
    @Operation(summary = "批量记账分配")
    public boolean batchPost(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costAllocationService.batchPost(tenantId, period);
    }
    
    @GetMapping("/allocation/summary/{period}")
    @Operation(summary = "获取分配汇总")
    public Map<String, Object> getAllocationSummary(@PathVariable String period) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costAllocationService.getAllocationSummary(tenantId, period);
    }
    
    @GetMapping("/rule/page")
    @Operation(summary = "分页查询分配规则")
    public Page<CostAllocationRule> pageRule(
            @Parameter(description = "来源中心ID") @RequestParam(required = false) Long fromCenterId,
            @Parameter(description = "启用状态") @RequestParam(required = false) Integer enabled,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<CostAllocationRule> page = new Page<>(pageNum, pageSize);
        return costAllocationRuleService.pageList(tenantId, fromCenterId, enabled, page);
    }
    
    @GetMapping("/rule/list")
    @Operation(summary = "获取所有启用的分配规则")
    public List<CostAllocationRule> listRules() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costAllocationRuleService.listAllEnabled(tenantId);
    }
    
    @PostMapping("/rule")
    @Operation(summary = "创建分配规则")
    public boolean createRule(@RequestBody CostAllocationRule rule) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        rule.setTenantId(tenantId);
        return costAllocationRuleService.createRule(rule);
    }
    
    @PutMapping("/rule/{id}")
    @Operation(summary = "更新分配规则")
    public boolean updateRule(@PathVariable Long id, @RequestBody CostAllocationRule rule) {
        rule.setId(id);
        return costAllocationRuleService.updateRule(rule);
    }
    
    @DeleteMapping("/rule/{id}")
    @Operation(summary = "删除分配规则")
    public boolean deleteRule(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costAllocationRuleService.deleteRule(tenantId, id);
    }
    
    @PutMapping("/rule/{id}/enable")
    @Operation(summary = "启用分配规则")
    public boolean enableRule(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costAllocationRuleService.enableRule(tenantId, id);
    }
    
    @PutMapping("/rule/{id}/disable")
    @Operation(summary = "停用分配规则")
    public boolean disableRule(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return costAllocationRuleService.disableRule(tenantId, id);
    }
    
    @GetMapping("/types")
    @Operation(summary = "获取成本类型列表")
    public List<CostType> listCostTypes() {
        return List.of(CostType.values());
    }
    
    @GetMapping("/center-types")
    @Operation(summary = "获取成本中心类型列表")
    public List<CostCenterType> listCenterTypes() {
        return List.of(CostCenterType.values());
    }
    
    @GetMapping("/allocation-methods")
    @Operation(summary = "获取分配方法列表")
    public List<CostAllocationMethod> listAllocationMethods() {
        return List.of(CostAllocationMethod.values());
    }
}