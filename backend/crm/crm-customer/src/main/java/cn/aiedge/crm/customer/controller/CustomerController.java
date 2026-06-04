package cn.aiedge.crm.customer.controller;

import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.service.CustomerService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@Tag(name = "CRM客户管理", description = "客户信息管理、查询、维护")
@RequiredArgsConstructor
public class CustomerController {
    
    private final CustomerService customerService;
    
    @Operation(summary = "分页查询客户列表")
    @GetMapping("/page")
    public Page<Customer> pageList(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户类型") @RequestParam(required = false) Integer customerType,
            @Parameter(description = "客户等级") @RequestParam(required = false) Integer customerLevel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "销售人员ID") @RequestParam(required = false) Long salesPersonId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        return customerService.pageList(keyword, customerType, customerLevel, status, salesPersonId, pageNum, pageSize);
    }
    
    @Operation(summary = "获取客户详情")
    @GetMapping("/{id}")
    public Customer getDetail(@PathVariable Long id) {
        return customerService.getById(id);
    }
    
    @Operation(summary = "根据编码查询客户")
    @GetMapping("/code/{customerCode}")
    public Customer getByCode(@PathVariable String customerCode) {
        return customerService.getByCustomerCode(customerCode);
    }
    
    @Operation(summary = "创建客户")
    @PostMapping
    public Customer create(@RequestBody Customer customer) {
        customer.setCustomerCode(customerService.generateCustomerCode());
        customerService.save(customer);
        return customer;
    }
    
    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        customerService.updateById(customer);
        return customer;
    }
    
    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return customerService.removeById(id);
    }

    @Operation(summary = "批量删除客户")
    @DeleteMapping("/batch")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return customerService.removeBatchByIds(ids);
    }

    @Operation(summary = "导出客户列表")
    @GetMapping("/export")
    public List<Customer> export(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "客户类型") @RequestParam(required = false) Integer customerType,
            @Parameter(description = "客户等级") @RequestParam(required = false) Integer customerLevel,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "销售人员ID") @RequestParam(required = false) Long salesPersonId) {
        return customerService.exportList(keyword, customerType, customerLevel, status, salesPersonId);
    }

    @Operation(summary = "查询销售人员的客户")
    @GetMapping("/salesPerson/{salesPersonId}")
    public List<Customer> listBySalesPerson(@PathVariable Long salesPersonId) {
        return customerService.listBySalesPersonId(salesPersonId);
    }
    
    @Operation(summary = "按等级查询客户")
    @GetMapping("/level/{customerLevel}")
    public List<Customer> listByLevel(@PathVariable Integer customerLevel) {
        return customerService.listByCustomerLevel(customerLevel);
    }
}