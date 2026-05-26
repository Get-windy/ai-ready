package cn.aiedge.erp.customer.controller;

import cn.aiedge.erp.customer.entity.CustomerLevel;
import cn.aiedge.erp.customer.service.CustomerLevelService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/erp/customer/level")
@Tag(name = "客户等级管理", description = "客户等级的增删改查和等级计算")
public class CustomerLevelController {
    
    @Autowired
    private CustomerLevelService customerLevelService;
    
    @GetMapping("/list")
    @Operation(summary = "获取所有等级")
    public List<CustomerLevel> listAll() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return customerLevelService.listAll(tenantId);
    }
    
    @GetMapping("/list/enabled")
    @Operation(summary = "获取启用的等级")
    public List<CustomerLevel> listEnabled() {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return customerLevelService.listEnabled(tenantId);
    }
    
    @GetMapping("/page")
    @Operation(summary = "分页查询等级")
    public Page<CustomerLevel> page(
            @Parameter(description = "等级名称") @RequestParam(required = false) String levelName,
            @Parameter(description = "等级编码") @RequestParam(required = false) String levelCode,
            @Parameter(description = "启用状态") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        Page<CustomerLevel> page = new Page<>(pageNum, pageSize);
        return customerLevelService.pageList(tenantId, levelName, levelCode, enabled, page);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取等级详情")
    public CustomerLevel getById(@PathVariable Long id) {
        return customerLevelService.getById(id);
    }
    
    @GetMapping("/code/{code}")
    @Operation(summary = "按编码获取等级")
    public CustomerLevel getByCode(@PathVariable String code) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return customerLevelService.getByCode(tenantId, code);
    }
    
    @PostMapping
    @Operation(summary = "创建等级")
    public boolean create(@RequestBody CustomerLevel level) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        level.setTenantId(tenantId);
        return customerLevelService.createLevel(level);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "更新等级")
    public boolean update(@PathVariable Long id, @RequestBody CustomerLevel level) {
        level.setId(id);
        return customerLevelService.updateLevel(level);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "删除等级")
    public boolean delete(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return customerLevelService.deleteLevel(tenantId, id);
    }
    
    @PutMapping("/{id}/enable")
    @Operation(summary = "启用等级")
    public boolean enable(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return customerLevelService.enableLevel(tenantId, id);
    }
    
    @PutMapping("/{id}/disable")
    @Operation(summary = "停用等级")
    public boolean disable(@PathVariable Long id) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return customerLevelService.disableLevel(tenantId, id);
    }
    
    @PostMapping("/calculate")
    @Operation(summary = "计算客户等级")
    public CustomerLevel calculateLevel(
            @Parameter(description = "交易金额") @RequestParam Double totalAmount,
            @Parameter(description = "交易频次") @RequestParam Integer frequency,
            @Parameter(description = "回款及时率") @RequestParam Integer paymentRate) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return customerLevelService.calculateCustomerLevel(tenantId, totalAmount, frequency, paymentRate);
    }
    
    @PostMapping("/assign/{customerId}")
    @Operation(summary = "为客户分配等级")
    public boolean assignLevel(@PathVariable Long customerId) {
        Long tenantId = StpUtil.getLoginIdAsLong();
        return customerLevelService.assignLevelToCustomer(tenantId, customerId);
    }
}