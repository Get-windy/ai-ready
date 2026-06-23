package cn.aiedge.erp.expense.controller;

import cn.aiedge.erp.expense.dto.ApiResponse;
import cn.aiedge.erp.expense.model.enumeration.ExpenseType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 费用类型管理控制器
 * 实现1个费用类型API
 */
@RestController
@RequestMapping("/api/erp/expense/type")
@Tag(name = "费用类型管理", description = "费用类型查询和配置")
public class ExpenseTypeController {
    
    /**
     * 费用类型列表
     * GET /api/erp/expense/type/list
     */
    @Operation(summary = "费用类型列表", description = "获取所有费用类型")
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> getExpenseTypeList() {
        List<Map<String, Object>> types = Arrays.stream(ExpenseType.values())
            .map(type -> {
                Map<String, Object> map = new HashMap<>();
                map.put("code", type.getCode());
                map.put("description", type.getDescription());
                map.put("order", type.getOrder());
                map.put("requiresApproval", type.isRequiresApproval());
                map.put("requiresInvoice", type.requiresInvoice());
                map.put("requiresBudgetControl", type.requiresBudgetControl());
                map.put("defaultApprovalLevel", type.getDefaultApprovalLevel());
                return map;
            })
            .collect(Collectors.toList());
        
        return ApiResponse.success(types);
    }
    
    /**
     * 获取费用类型详情
     */
    @Operation(summary = "获取费用类型详情", description = "根据code获取费用类型详情")
    @GetMapping("/{code}")
    public ApiResponse<Map<String, Object>> getExpenseTypeDetail(@PathVariable String code) {
        ExpenseType type = ExpenseType.fromCode(code);
        Map<String, Object> typeDetail = new HashMap<>();
        typeDetail.put("code", type.getCode());
        typeDetail.put("description", type.getDescription());
        typeDetail.put("order", type.getOrder());
        typeDetail.put("requiresApproval", type.isRequiresApproval());
        typeDetail.put("requiresInvoice", type.requiresInvoice());
        typeDetail.put("requiresBudgetControl", type.requiresBudgetControl());
        typeDetail.put("defaultApprovalLevel", type.getDefaultApprovalLevel());
        
        return ApiResponse.success(typeDetail);
    }
    
    /**
     * 获取费用类型描述列表（用于下拉框）
     */
    @Operation(summary = "费用类型描述列表", description = "获取所有费用类型的描述，用于前端下拉框")
    @GetMapping("/descriptions")
    public ApiResponse<List<String>> getExpenseTypeDescriptions() {
        List<String> descriptions = Arrays.asList(ExpenseType.getAllDescriptions());
        return ApiResponse.success(descriptions);
    }
    
    /**
     * 获取费用类型代码列表
     */
    @Operation(summary = "费用类型代码列表", description = "获取所有费用类型的代码")
    @GetMapping("/codes")
    public ApiResponse<List<String>> getExpenseTypeCodes() {
        List<String> codes = Arrays.asList(ExpenseType.getAllCodes());
        return ApiResponse.success(codes);
    }
}