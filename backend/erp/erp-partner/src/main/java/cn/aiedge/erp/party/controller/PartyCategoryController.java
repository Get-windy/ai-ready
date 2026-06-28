package cn.aiedge.erp.party.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.party.entity.PartyCategory;
import cn.aiedge.erp.party.service.IPartyCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "往来单位分类管理", description = "往来单位分类CRUD接口")
@RestController
@RequestMapping("/api/erp/partner/categories")
@RequiredArgsConstructor
public class PartyCategoryController {

    private final IPartyCategoryService partyCategoryService;

    // 类型字符串映射到整数
    private static final Map<String, Integer> TYPE_MAP = Map.of(
        "CUSTOMER", 1,
        "SUPPLIER", 2,
        "LOGISTICS", 3,
        "OTHER", 4
    );

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<PartyCategory>>> getCategoryTree(
            @RequestParam(required = false) String categoryType) {
        Integer typeCode = null;
        if (categoryType != null && !categoryType.isEmpty()) {
            typeCode = TYPE_MAP.getOrDefault(categoryType.toUpperCase(), null);
        }
        List<PartyCategory> categories = partyCategoryService.getCategoryTree(typeCode);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @Operation(summary = "根据类型获取分类列表")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PartyCategory>>> getCategoryList(
            @RequestParam(required = false) String categoryType) {
        Integer typeCode = null;
        if (categoryType != null && !categoryType.isEmpty()) {
            typeCode = TYPE_MAP.getOrDefault(categoryType.toUpperCase(), null);
        }
        List<PartyCategory> categories = partyCategoryService.getCategoryListByType(typeCode);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }
}