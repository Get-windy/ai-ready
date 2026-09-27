package cn.aiedge.crm.customer.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.crm.customer.entity.Customer;
import cn.aiedge.crm.customer.entity.CustomerFollowUp;
import cn.aiedge.crm.customer.service.CustomerFollowUpService;
import cn.aiedge.crm.customer.service.CustomerService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@RestController
// 与同模块其余 9 个控制器一致（都在 /api/crm/* 下）。此前是 CRM 里唯一不守约定的前缀，
// 会误导「URL 前缀 → 模块」类工具推出 customer:* 而非 crm:customer:*
@RequestMapping("/api/crm/customer")
@Tag(name = "CRM客户管理", description = "客户信息管理、查询、维护")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerFollowUpService customerFollowUpService;
    
    @Operation(summary = "分页查询客户列表")
    @SaCheckPermission("crm:customer:list")
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
    @SaCheckPermission("crm:customer:view")
    @GetMapping("/{id}")
    public Customer getDetail(@PathVariable Long id) {
        // 空值语义与同模块其它控制器（contract/quotation/marketing）统一：不存在报 404，
        // 不再返回 200 + 空响应体（前端须写两套判空，对外语义也不规范）
        Customer customer = customerService.getById(id);
        if (customer == null) {
            throw BusinessException.notFound("客户不存在");
        }
        return customer;
    }
    
    @Operation(summary = "根据编码查询客户")
    @SaCheckPermission("crm:customer:view")
    @GetMapping("/code/{customerCode}")
    public Customer getByCode(@PathVariable String customerCode) {
        return customerService.getByCustomerCode(customerCode);
    }
    
    @Operation(summary = "创建客户")
    @SaCheckPermission("crm:customer:create")
    @PostMapping
    public Customer create(@RequestBody Customer customer) {
        customer.setCustomerCode(customerService.generateCustomerCode());
        customerService.save(customer);
        return customer;
    }
    
    @Operation(summary = "更新客户")
    @SaCheckPermission("crm:customer:update")
    @PutMapping("/{id}")
    public Customer update(@PathVariable Long id, @RequestBody Customer customer) {
        customer.setId(id);
        customerService.updateById(customer);
        return customer;
    }
    
    @Operation(summary = "删除客户")
    @SaCheckPermission("crm:customer:delete")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return customerService.removeById(id);
    }

    @Operation(summary = "批量删除客户")
    @SaCheckPermission("crm:customer:delete")
    @DeleteMapping("/batch")
    public boolean batchDelete(@RequestBody List<Long> ids) {
        return customerService.removeBatchByIds(ids);
    }

    @Operation(summary = "导出客户列表")
    @SaCheckPermission("crm:customer:list")
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
    @SaCheckPermission("crm:customer:view")
    @GetMapping("/salesPerson/{salesPersonId:\\d+}")
    public List<Customer> listBySalesPerson(@PathVariable Long salesPersonId) {
        return customerService.listBySalesPersonId(salesPersonId);
    }
    
    @Operation(summary = "按等级查询客户")
    @SaCheckPermission("crm:customer:view")
    @GetMapping("/level/{customerLevel:\\d+}")
    public List<Customer> listByLevel(@PathVariable Integer customerLevel) {
        return customerService.listByCustomerLevel(customerLevel);
    }

    @Operation(summary = "获取客户选项列表（下拉选择用）")
    @SaCheckPermission("crm:customer:view")
    @GetMapping("/dropdown")
    public List<java.util.Map<String, Object>> getOptions(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Customer> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.select(Customer::getId, Customer::getCustomerName);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Customer::getCustomerName, keyword);
        }
        wrapper.eq(Customer::getStatus, 1);
        wrapper.last("LIMIT 200");
        List<Customer> customers = customerService.list(wrapper);
        return customers.stream().map(c -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getCustomerName());
            return map;
        }).toList();
    }

    @Operation(summary = "获取客户列表（无分页，供下拉选择器使用）")
    @SaCheckPermission("crm:customer:list")
    @GetMapping("/list")
    public List<Customer> list(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        return customerService.list();
    }

    @Operation(summary = "更新客户状态")
    @SaCheckPermission("crm:customer:update")
    @PutMapping("/{id:\\d+}/status")
    public Customer updateStatus(
            @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        Customer customer = customerService.getById(id);
        if (customer != null) {
            customer.setStatus(status);
            customerService.updateById(customer);
        }
        return customer;
    }

    @Operation(summary = "导入客户")
    @SaCheckPermission("crm:customer:import")
    @PostMapping("/import")
    public boolean importCustomers(@RequestBody List<Customer> customers) {
        for (Customer customer : customers) {
            customer.setCustomerCode(customerService.generateCustomerCode());
            customerService.save(customer);
        }
        return true;
    }

    @Operation(summary = "获取客户跟进记录")
    @SaCheckPermission("crm:customer:view")
    @GetMapping("/{customerId:\\d+}/follows")
    public List<CustomerFollowUp> getFollowRecords(@PathVariable Long customerId) {
        return customerFollowUpService.listByCustomerId(customerId);
    }

    @Operation(summary = "添加客户跟进记录")
    @SaCheckPermission("crm:customer:follow")
    @PostMapping("/{customerId:\\d+}/follow")
    public CustomerFollowUp addFollowRecord(
            @PathVariable Long customerId,
            @RequestBody CustomerFollowUp followUp) {
        followUp.setCustomerId(customerId);
        followUp.setFollowUpCode(customerFollowUpService.generateFollowUpCode());
        customerFollowUpService.save(followUp);
        return followUp;
    }

    @Operation(summary = "获取客户订单记录")
    @SaCheckPermission("crm:customer:view")
    @GetMapping("/{customerId:\\d+}/orders")
    public List<?> getOrderRecords(@PathVariable Long customerId) {
        // 订单记录由销售模块提供，此处返回空列表
        return List.of();
    }
}