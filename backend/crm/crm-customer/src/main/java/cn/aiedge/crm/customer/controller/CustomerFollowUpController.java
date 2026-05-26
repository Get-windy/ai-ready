package cn.aiedge.crm.customer.controller;

import cn.aiedge.crm.customer.entity.CustomerFollowUp;
import cn.aiedge.crm.customer.service.CustomerFollowUpService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crm/followUp")
@Tag(name = "CRM跟进记录", description = "客户跟进记录管理")
@RequiredArgsConstructor
public class CustomerFollowUpController {
    
    private final CustomerFollowUpService customerFollowUpService;
    
    @Operation(summary = "分页查询跟进记录")
    @GetMapping("/page")
    public Page<CustomerFollowUp> pageList(
            @Parameter(description = "客户ID") @RequestParam(required = false) Long customerId,
            @Parameter(description = "商机ID") @RequestParam(required = false) Long opportunityId,
            @Parameter(description = "线索ID") @RequestParam(required = false) Long leadId,
            @Parameter(description = "销售人员ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return customerFollowUpService.pageList(customerId, opportunityId, leadId, salesPersonId, pageNum, pageSize);
    }
    
    @Operation(summary = "获取跟进记录详情")
    @GetMapping("/{id}")
    public CustomerFollowUp getDetail(@PathVariable Long id) {
        return customerFollowUpService.getById(id);
    }
    
    @Operation(summary = "根据编码查询跟进记录")
    @GetMapping("/code/{followUpCode}")
    public CustomerFollowUp getByCode(@PathVariable String followUpCode) {
        return customerFollowUpService.getByFollowUpCode(followUpCode);
    }
    
    @Operation(summary = "创建跟进记录")
    @PostMapping
    public CustomerFollowUp create(@RequestBody CustomerFollowUp followUp) {
        followUp.setFollowUpCode(customerFollowUpService.generateFollowUpCode());
        customerFollowUpService.save(followUp);
        return followUp;
    }
    
    @Operation(summary = "更新跟进记录")
    @PutMapping("/{id}")
    public CustomerFollowUp update(@PathVariable Long id, @RequestBody CustomerFollowUp followUp) {
        followUp.setId(id);
        customerFollowUpService.updateById(followUp);
        return followUp;
    }
    
    @Operation(summary = "删除跟进记录")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return customerFollowUpService.removeById(id);
    }
    
    @Operation(summary = "查询客户的跟进记录")
    @GetMapping("/customer/{customerId}")
    public List<CustomerFollowUp> listByCustomer(@PathVariable Long customerId) {
        return customerFollowUpService.listByCustomerId(customerId);
    }
    
    @Operation(summary = "查询商机的跟进记录")
    @GetMapping("/opportunity/{opportunityId}")
    public List<CustomerFollowUp> listByOpportunity(@PathVariable Long opportunityId) {
        return customerFollowUpService.listByOpportunityId(opportunityId);
    }
    
    @Operation(summary = "查询线索的跟进记录")
    @GetMapping("/lead/{leadId}")
    public List<CustomerFollowUp> listByLead(@PathVariable Long leadId) {
        return customerFollowUpService.listByLeadId(leadId);
    }
}