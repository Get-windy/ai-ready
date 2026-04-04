package cn.aiedge.customer.controller;

import cn.aiedge.customer.dto.CustomerDTO;
import cn.aiedge.customer.dto.CustomerFollowDTO;
import cn.aiedge.customer.entity.Customer;
import cn.aiedge.customer.service.ICustomerService;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户管理控制器
 */
@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final ICustomerService customerService;

    @Operation(summary = "分页查询客户")
    @GetMapping("/page")
    @SaCheckLogin
    public Result<Page<CustomerDTO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Long ownerId) {
        Page<Customer> page = new Page<>(pageNum, pageSize);
        Page<CustomerDTO> result = customerService.pageCustomers(page, tenantId, customerName, stage, level, ownerId);
        return Result.ok(result);
    }

    @Operation(summary = "获取客户详情")
    @GetMapping("/{id}")
    @SaCheckLogin
    public Result<CustomerDTO> getDetail(@PathVariable Long id) {
        CustomerDTO dto = customerService.getCustomerDetail(id);
        return Result.ok(dto);
    }

    @Operation(summary = "创建客户")
    @PostMapping
    @SaCheckPermission("customer:create")
    public Result<Long> create(@RequestBody CustomerDTO dto) {
        Long id = customerService.createCustomer(dto);
        return Result.ok("创建成功", id);
    }

    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    @SaCheckPermission("customer:update")
    public Result<Void> update(@PathVariable Long id, @RequestBody CustomerDTO dto) {
        dto.setId(id);
        customerService.updateCustomer(dto);
        return Result.ok("更新成功", null);
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    @SaCheckPermission("customer:delete")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return Result.ok("删除成功", null);
    }

    @Operation(summary = "更新客户阶段")
    @PutMapping("/{id}/stage")
    @SaCheckPermission("customer:update")
    public Result<Void> updateStage(@PathVariable Long id, @RequestParam Integer stage) {
        customerService.updateStage(id, stage);
        return Result.ok("更新成功", null);
    }

    @Operation(summary = "转移客户")
    @PutMapping("/{id}/transfer")
    @SaCheckPermission("customer:transfer")
    public Result<Void> transfer(@PathVariable Long id, @RequestParam Long newOwnerId) {
        customerService.transferCustomer(id, newOwnerId);
        return Result.ok("转移成功", null);
    }

    @Operation(summary = "待跟进客户列表")
    @GetMapping("/pending-follow")
    @SaCheckLogin
    public Result<List<CustomerDTO>> getPendingFollow(@RequestParam Long ownerId) {
        List<CustomerDTO> list = customerService.getPendingFollowCustomers(ownerId);
        return Result.ok(list);
    }

    @Operation(summary = "按阶段查询客户")
    @GetMapping("/by-stage")
    @SaCheckLogin
    public Result<List<CustomerDTO>> getByStage(@RequestParam Long tenantId, @RequestParam Integer stage) {
        List<CustomerDTO> list = customerService.getCustomersByStage(tenantId, stage);
        return Result.ok(list);
    }

    @Operation(summary = "添加跟进记录")
    @PostMapping("/{id}/follow")
    @SaCheckPermission("customer:follow")
    public Result<Long> addFollow(@PathVariable Long id, @RequestBody CustomerFollowDTO dto) {
        dto.setCustomerId(id);
        Long followId = customerService.addFollowRecord(dto);
        return Result.ok("添加成功", followId);
    }

    @Operation(summary = "获取跟进记录")
    @GetMapping("/{id}/follows")
    @SaCheckLogin
    public Result<List<CustomerFollowDTO>> getFollows(@PathVariable Long id) {
        List<CustomerFollowDTO> list = customerService.getFollowRecords(id);
        return Result.ok(list);
    }

    public static class Result<T> {
        private int code;
        private String message;
        private T data;

        public static <T> Result<T> ok(T data) {
            Result<T> result = new Result<>();
            result.code = 200;
            result.message = "success";
            result.data = data;
            return result;
        }

        public static <T> Result<T> ok(String message, T data) {
            Result<T> result = new Result<>();
            result.code = 200;
            result.message = message;
            result.data = data;
            return result;
        }
    }
}