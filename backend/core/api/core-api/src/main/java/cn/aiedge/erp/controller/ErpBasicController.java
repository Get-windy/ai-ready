package cn.aiedge.erp.controller;

import cn.aiedge.base.service.UserService;
import cn.aiedge.common.result.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP基础数据控制器
 * 提供销售人员、仓库等基础数据查询接口
 */
@Tag(name = "ERP基础数据", description = "ERP基础数据查询接口")
@RestController
@RequestMapping("/api/erp/basic")
@RequiredArgsConstructor
public class ErpBasicController {

    private final UserService userService;

    @Operation(summary = "获取销售人员列表")
    @GetMapping("/salespersons")
    public ApiResponse<List<Map<String, Object>>> getSalespersons() {
        // 返回销售人员列表（从用户系统获取或使用演示数据）
        List<Map<String, Object>> salespersons = new ArrayList<>();

        // 添加演示数据（实际项目应从用户/员工表中筛选）
        salespersons.add(createSalesperson(1L, "张三"));
        salespersons.add(createSalesperson(2L, "李四"));
        salespersons.add(createSalesperson(3L, "王五"));
        salespersons.add(createSalesperson(4L, "赵六"));
        salespersons.add(createSalesperson(5L, "陈七"));

        return ApiResponse.ok(salespersons);
    }

    @Operation(summary = "获取仓库列表")
    @GetMapping("/warehouses")
    public ApiResponse<List<Map<String, Object>>> getWarehouses() {
        List<Map<String, Object>> warehouses = new ArrayList<>();

        warehouses.add(createWarehouse(1L, "主仓库"));
        warehouses.add(createWarehouse(2L, "备用仓库"));
        warehouses.add(createWarehouse(3L, "临时仓库"));

        return ApiResponse.ok(warehouses);
    }

    private Map<String, Object> createSalesperson(Long id, String name) {
        Map<String, Object> person = new HashMap<>();
        person.put("id", id);
        person.put("name", name);
        return person;
    }

    private Map<String, Object> createWarehouse(Long id, String name) {
        Map<String, Object> warehouse = new HashMap<>();
        warehouse.put("id", id);
        warehouse.put("name", name);
        return warehouse;
    }
}