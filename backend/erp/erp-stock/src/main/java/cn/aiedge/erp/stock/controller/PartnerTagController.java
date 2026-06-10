package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerTag;
import cn.aiedge.erp.stock.service.PartnerTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "往来单位标签管理")
@RestController
@RequestMapping("/api/erp/partner/tags")
@RequiredArgsConstructor
public class PartnerTagController {

    private final PartnerTagService partnerTagService;

    @Operation(summary = "查询标签列表")
    @GetMapping
    public Result<List<PartnerTag>> list() {
        return Result.ok(partnerTagService.lambdaQuery().eq(PartnerTag::getDeleted, 0).list());
    }

    @Operation(summary = "新增标签")
    @PostMapping
    public Result<Boolean> create(@RequestBody PartnerTag tag) {
        return Result.ok(partnerTagService.save(tag));
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PartnerTag tag) {
        tag.setId(id);
        return Result.ok(partnerTagService.updateById(tag));
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(partnerTagService.removeById(id));
    }

    @Operation(summary = "查询客户标签ID列表")
    @GetMapping("/partner/{partnerId}")
    public Result<List<Long>> getTagIds(@PathVariable Long partnerId) {
        return Result.ok(partnerTagService.getTagIdsByPartner(partnerId));
    }

    @Operation(summary = "批量设置客户标签")
    @PostMapping("/attach")
    public Result<Boolean> attachTags(@RequestBody AttachTagsRequest request) {
        return Result.ok(partnerTagService.attachTags(request.getPartnerId(), request.getTagIds()));
    }

    @lombok.Data
    public static class AttachTagsRequest {
        private Long partnerId;
        private List<Long> tagIds;
    }
}
