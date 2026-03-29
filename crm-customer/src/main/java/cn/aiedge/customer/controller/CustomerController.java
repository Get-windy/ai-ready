package cn.aiedge.customer.controller;

import cn.aiedge.base.vo.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户管理控制器
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "客户管理", description = "客户CRUD接口")
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    /**
     * 获取客户列表
     */
    @Operation(summary = "获取客户列表")
    @GetMapping
    public Result<?> listCustomers() {
        return Result.ok("客户列表", null);
    }

    /**
     * 获取客户详情
     */
    @Operation(summary = "获取客户详情")
    @GetMapping("/{id}")
    public Result<?> getCustomer(@PathVariable Long id) {
        return Result.ok("客户详情", null);
    }

    /**
     * 创建客户
     */
    @Operation(summary = "创建客户")
    @PostMapping
    public Result<?> createCustomer(@RequestBody Object dto) {
        return Result.ok("创建成功", null);
    }

    /**
     * 更新客户
     */
    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    public Result<?> updateCustomer(@PathVariable Long id, @RequestBody Object dto) {
        return Result.ok("更新成功", null);
    }

    /**
     * 删除客户
     */
    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public Result<?> deleteCustomer(@PathVariable Long id) {
        return Result.ok("删除成功", null);
    }
}
