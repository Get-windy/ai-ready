package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.MallTag;
import cn.aiedge.erp.stock.service.MallTagService;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商城标签Controller
 */
@Slf4j
@Tag(name = "商城标签管理")
@RestController
@RequestMapping("/api/erp/mall-tag")
@RequiredArgsConstructor
public class MallTagController {

    private final MallTagService mallTagService;

    @Operation(summary = "获取所有标签（当前租户）")
    @GetMapping("/list")
    public Result<List<MallTag>> list() {
        long tenantId = StpUtil.getExtra("tenantId") != null
                ? Long.parseLong(StpUtil.getExtra("tenantId").toString()) : 1L;
        return Result.ok(mallTagService.getByTenantId(tenantId));
    }

    @Operation(summary = "创建标签")
    @PostMapping
    public Result<Boolean> create(@RequestBody MallTag tag) {
        long tenantId = StpUtil.getExtra("tenantId") != null
                ? Long.parseLong(StpUtil.getExtra("tenantId").toString()) : 1L;
        tag.setTenantId(tenantId);
        tag.setId(null);
        return Result.ok(mallTagService.save(tag));
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody MallTag tag) {
        tag.setId(id);
        return Result.ok(mallTagService.updateById(tag));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(mallTagService.removeById(id));
    }
}
