package cn.aiedge.crm.customer.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.crm.customer.dto.CustomerFollowUpQuery;
import cn.aiedge.crm.customer.entity.CustomerFollowUp;
import cn.aiedge.crm.customer.service.CustomerFollowUpService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@RestController
@RequestMapping("/api/crm/followUp")
@Tag(name = "CRM跟进记录", description = "客户跟进记录管理")
@RequiredArgsConstructor
public class CustomerFollowUpController {
    
    private final CustomerFollowUpService customerFollowUpService;
    
    @Operation(summary = "分页查询跟进记录")
    @SaCheckPermission("crm:follow-up:list")
    @GetMapping("/page")
    public Page<CustomerFollowUp> pageList(CustomerFollowUpQuery query) {
        return customerFollowUpService.pageList(query);
    }
    
    @Operation(summary = "获取跟进记录详情")
    @SaCheckPermission("crm:follow-up:detail")
    @GetMapping("/{id}")
    public CustomerFollowUp getDetail(@PathVariable Long id) {
        // 空值语义与同模块其它控制器统一：不存在报 404，不再返回 200 + 空响应体
        CustomerFollowUp followUp = customerFollowUpService.getById(id);
        if (followUp == null) {
            throw BusinessException.notFound("跟进记录不存在");
        }
        return followUp;
    }
    
    @Operation(summary = "根据编码查询跟进记录")
    @SaCheckPermission("crm:follow-up:detail")
    @GetMapping("/code/{followUpCode}")
    public CustomerFollowUp getByCode(@PathVariable String followUpCode) {
        return customerFollowUpService.getByFollowUpCode(followUpCode);
    }
    
    @Operation(summary = "创建跟进记录")
    @SaCheckPermission("crm:follow-up:create")
    @PostMapping
    public CustomerFollowUp create(@RequestBody CustomerFollowUp followUp) {
        followUp.setFollowUpCode(customerFollowUpService.generateFollowUpCode());
        customerFollowUpService.save(followUp);
        return followUp;
    }
    
    @Operation(summary = "更新跟进记录")
    @SaCheckPermission("crm:follow-up:update")
    @PutMapping("/{id}")
    public CustomerFollowUp update(@PathVariable Long id, @RequestBody CustomerFollowUp followUp) {
        followUp.setId(id);
        customerFollowUpService.updateById(followUp);
        return followUp;
    }
    
    @Operation(summary = "删除跟进记录")
    @SaCheckPermission("crm:follow-up:delete")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return customerFollowUpService.removeById(id);
    }
    
    @Operation(summary = "查询客户的跟进记录")
    @SaCheckPermission("crm:follow-up:detail")
    @GetMapping("/customer/{customerId}")
    public List<CustomerFollowUp> listByCustomer(@PathVariable Long customerId) {
        return customerFollowUpService.listByCustomerId(customerId);
    }
    
    @Operation(summary = "查询商机的跟进记录")
    @SaCheckPermission("crm:follow-up:detail")
    @GetMapping("/opportunity/{opportunityId}")
    public List<CustomerFollowUp> listByOpportunity(@PathVariable Long opportunityId) {
        return customerFollowUpService.listByOpportunityId(opportunityId);
    }
    
    @Operation(summary = "查询线索的跟进记录")
    @SaCheckPermission("crm:lead:view")
    @GetMapping("/lead/{leadId}")
    public List<CustomerFollowUp> listByLead(@PathVariable Long leadId) {
        return customerFollowUpService.listByLeadId(leadId);
    }
}