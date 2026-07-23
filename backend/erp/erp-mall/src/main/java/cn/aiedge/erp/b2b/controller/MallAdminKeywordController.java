package cn.aiedge.erp.b2b.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.b2b.model.MallKeyword;
import cn.aiedge.erp.b2b.service.MallKeywordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商城搜索关键词库管理（管理后台）
 */
@RestController
@RequestMapping("/api/erp/mall/admin/keyword")
@Tag(name = "商城关键词库", description = "商城搜索关键词的分页/新增/启停管理接口")
@RequiredArgsConstructor
public class MallAdminKeywordController {

    private final MallKeywordService mallKeywordService;

    @GetMapping("/page")
    @Operation(summary = "分页查询关键词")
    public Result<IPage<MallKeyword>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "关键词(模糊)") @RequestParam(required = false) String keyword,
            @Parameter(description = "类型 1热门 2置顶 3屏蔽") @RequestParam(required = false) Integer keywordType,
            @Parameter(description = "状态 1启用 0禁用") @RequestParam(required = false) Integer status) {
        return Result.ok(mallKeywordService.pageKeywords(pageNum, pageSize, keyword, keywordType, status));
    }

    @PostMapping
    @Operation(summary = "新增关键词")
    public Result<Void> create(@RequestBody MallKeyword keyword) {
        mallKeywordService.createKeyword(keyword);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新关键词")
    public Result<Void> update(@PathVariable Long id, @RequestBody MallKeyword keyword) {
        keyword.setId(id);
        mallKeywordService.updateKeyword(keyword);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除关键词")
    public Result<Void> delete(@PathVariable Long id) {
        mallKeywordService.deleteKeyword(id);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "启用/禁用关键词")
    public Result<Void> toggleStatus(
            @PathVariable Long id,
            @Parameter(description = "状态 1启用 0禁用") @RequestParam Integer status) {
        mallKeywordService.toggleStatus(id, status);
        return Result.ok();
    }
}
