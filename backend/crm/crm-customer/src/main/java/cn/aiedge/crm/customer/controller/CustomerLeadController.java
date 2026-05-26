package cn.aiedge.crm.customer.controller;

import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.entity.CustomerLead;
import cn.aiedge.crm.customer.service.CustomerLeadService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/crm/lead")
@Tag(name = "CRM销售线索", description = "销售线索管理、转化")
@RequiredArgsConstructor
public class CustomerLeadController {
    
    private final CustomerLeadService customerLeadService;
    
    @Operation(summary = "分页查询线索列表")
    @GetMapping("/page")
    public Page<CustomerLead> pageList(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "线索状态") @RequestParam(required = false) Integer leadStatus,
            @Parameter(description = "线索等级") @RequestParam(required = false) Integer leadLevel,
            @Parameter(description = "销售人员ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return customerLeadService.pageList(keyword, leadStatus, leadLevel, salesPersonId, pageNum, pageSize);
    }
    
    @Operation(summary = "获取线索详情")
    @GetMapping("/{id}")
    public CustomerLead getDetail(@PathVariable Long id) {
        return customerLeadService.getById(id);
    }
    
    @Operation(summary = "根据编码查询线索")
    @GetMapping("/code/{leadCode}")
    public CustomerLead getByCode(@PathVariable String leadCode) {
        return customerLeadService.getByLeadCode(leadCode);
    }
    
    @Operation(summary = "创建线索")
    @PostMapping
    public CustomerLead create(@RequestBody CustomerLead lead) {
        lead.setLeadCode(customerLeadService.generateLeadCode());
        customerLeadService.save(lead);
        return lead;
    }
    
    @Operation(summary = "更新线索")
    @PutMapping("/{id}")
    public CustomerLead update(@PathVariable Long id, @RequestBody CustomerLead lead) {
        lead.setId(id);
        customerLeadService.updateById(lead);
        return lead;
    }
    
    @Operation(summary = "删除线索")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return customerLeadService.removeById(id);
    }
    
    @Operation(summary = "转化线索为客户")
    @PostMapping("/{id}/convert")
    public Customer convertToCustomer(@PathVariable Long id) {
        return customerLeadService.convertToCustomer(id);
    }
    
    @Operation(summary = "查询销售人员的线索")
    @GetMapping("/salesPerson/{salesPersonId}")
    public List<CustomerLead> listBySalesPerson(@PathVariable Long salesPersonId) {
        return customerLeadService.listBySalesPersonId(salesPersonId);
    }
}