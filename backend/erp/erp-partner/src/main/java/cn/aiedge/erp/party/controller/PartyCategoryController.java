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

@Tag(name = "往来单位分类管理", description = "往来单位分类CRUD接口")
@RestController
@RequestMapping("/api/erp/partner/categories")
@RequiredArgsConstructor
public class PartyCategoryController {

    private final IPartyCategoryService partyCategoryService;

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<PartyCategory>>> getCategoryTree(@RequestParam(required = false) Integer categoryType) {
        List<PartyCategory> categories = partyCategoryService.getCategoryTree(categoryType);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @Operation(summary = "根据类型获取分类列表")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PartyCategory>>> getCategoryList(@RequestParam(required = false) Integer categoryType) {
        List<PartyCategory> categories = partyCategoryService.getCategoryListByType(categoryType);
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }
}