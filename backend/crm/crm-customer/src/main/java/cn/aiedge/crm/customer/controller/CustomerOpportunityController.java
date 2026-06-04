package cn.aiedge.crm.customer.controller;

import cn.aiedge.crm.customer.entity.CustomerOpportunity;
import cn.aiedge.crm.customer.service.CustomerOpportunityService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/crm/opportunity")
@Tag(name = "CRM商机管理", description = "商机管理、阶段推进、赢单输单")
@RequiredArgsConstructor
public class CustomerOpportunityController {
    
    private final CustomerOpportunityService customerOpportunityService;
    
    @Operation(summary = "分页查询商机列表")
    @GetMapping("/page")
    public Page<CustomerOpportunity> pageList(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "商机阶段") @RequestParam(required = false) Integer opportunityStage,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "销售人员ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return customerOpportunityService.pageList(keyword, customerId, opportunityStage, status, salesPersonId, pageNum, pageSize);
    }
    
    @Operation(summary = "获取商机详情")
    @GetMapping("/{id}")
    public CustomerOpportunity getDetail(@PathVariable Long id) {
        return customerOpportunityService.getById(id);
    }
    
    @Operation(summary = "根据编码查询商机")
    @GetMapping("/code/{opportunityCode}")
    public CustomerOpportunity getByCode(@PathVariable String opportunityCode) {
        return customerOpportunityService.getByOpportunityCode(opportunityCode);
    }
    
    @Operation(summary = "创建商机")
    @PostMapping
    public CustomerOpportunity create(@RequestBody CustomerOpportunity opportunity) {
        opportunity.setOpportunityCode(customerOpportunityService.generateOpportunityCode());
        customerOpportunityService.save(opportunity);
        return opportunity;
    }
    
    @Operation(summary = "更新商机")
    @PutMapping("/{id}")
    public CustomerOpportunity update(@PathVariable Long id, @RequestBody CustomerOpportunity opportunity) {
        opportunity.setId(id);
        customerOpportunityService.updateById(opportunity);
        return opportunity;
    }
    
    @Operation(summary = "删除商机")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return customerOpportunityService.removeById(id);
    }

    @Operation(summary = "批量删除商机")
    @DeleteMapping("/batch")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return customerOpportunityService.removeBatchByIds(ids);
    }

    @Operation(summary = "导出商机列表")
    @GetMapping("/export")
    public List<CustomerOpportunity> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "商机阶段") @RequestParam(required = false) Integer opportunityStage,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "销售人员ID") @RequestParam(required = false) Long salesPersonId) {
        return customerOpportunityService.exportList(keyword, customerId, opportunityStage, status, salesPersonId);
    }

    @Operation(summary = "推进商机阶段")
    @PostMapping("/{id}/advance")
    public CustomerOpportunity advanceStage(@PathVariable Long id) {
        return customerOpportunityService.advanceStage(id);
    }
    
    @Operation(summary = "商机赢单")
    @PostMapping("/{id}/win")
    public CustomerOpportunity winOpportunity(
            @PathVariable Long id,
            @Parameter(description = "实际金额") @RequestParam BigDecimal actualAmount) {
        return customerOpportunityService.winOpportunity(id, actualAmount);
    }
    
    @Operation(summary = "商机输单")
    @PostMapping("/{id}/lose")
    public CustomerOpportunity loseOpportunity(
            @PathVariable Long id,
            @Parameter(description = "输单原因") @RequestParam String loseReason) {
        return customerOpportunityService.loseOpportunity(id, loseReason);
    }
    
    @Operation(summary = "查询客户的商机")
    @GetMapping("/customer/{customerId}")
    public List<CustomerOpportunity> listByCustomer(@PathVariable Long customerId) {
        return customerOpportunityService.listByCustomerId(customerId);
    }
    
    @Operation(summary = "查询销售人员的商机")
    @GetMapping("/salesPerson/{salesPersonId}")
    public List<CustomerOpportunity> listBySalesPerson(@PathVariable Long salesPersonId) {
        return customerOpportunityService.listBySalesPersonId(salesPersonId);
    }
    
    @Operation(summary = "商机统计")
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics(
            @Parameter(description = "销售人员ID") @RequestParam(required = false) Long salesPersonId) {
        return customerOpportunityService.getOpportunityStatistics(salesPersonId);
    }
}