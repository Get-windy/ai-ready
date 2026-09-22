package cn.aiedge.erp.b2b.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.b2b.model.MallPopupAd;
import cn.aiedge.erp.b2b.service.MallPopupAdService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 弹窗广告管理（管理后台）
 */
@RestController
@RequestMapping("/api/erp/mall/admin/popup-ad")
@Tag(name = "弹窗广告管理", description = "弹窗广告的分页/创建/发布/下线等管理接口")
@RequiredArgsConstructor
public class MallAdminPopupAdController {

    private final MallPopupAdService mallPopupAdService;

    @SaCheckPermission("mall:popup-ad:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询弹窗广告")
    public Result<IPage<MallPopupAd>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "标题关键词") @RequestParam(required = false) String title,
            @Parameter(description = "展示方式") @RequestParam(required = false) String showType,
            @Parameter(description = "目标用户") @RequestParam(required = false) String targetUser,
            @Parameter(description = "状态 0草稿 1投放中 2已结束 3已下架") @RequestParam(required = false) Integer status) {
        Page<MallPopupAd> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MallPopupAd> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isEmpty()) {
            wrapper.like(MallPopupAd::getTitle, title);
        }
        if (showType != null && !showType.isEmpty()) {
            wrapper.eq(MallPopupAd::getShowType, showType);
        }
        if (targetUser != null && !targetUser.isEmpty()) {
            wrapper.eq(MallPopupAd::getTargetUser, targetUser);
        }
        if (status != null) {
            wrapper.eq(MallPopupAd::getStatus, status);
        }
        wrapper.orderByAsc(MallPopupAd::getSort).orderByDesc(MallPopupAd::getCreateTime);
        return Result.ok(mallPopupAdService.page(page, wrapper));
    }

    @SaCheckPermission("mall:popup-ad:detail")
    @GetMapping("/{id}")
    @Operation(summary = "查询弹窗广告详情")
    public Result<MallPopupAd> getById(@PathVariable Long id) {
        return Result.ok(mallPopupAdService.getById(id));
    }

    @SaCheckPermission("mall:popup-ad:create")
    @PostMapping
    @Operation(summary = "创建弹窗广告")
    public Result<Void> create(@RequestBody MallPopupAd popupAd) {
        popupAd.setId(null);
        popupAd.setStatus(MallPopupAd.STATUS_DRAFT);
        popupAd.setCreatorName(cn.aiedge.base.utils.SecurityUtils.getCurrentUsername());
        mallPopupAdService.save(popupAd);
        return Result.ok();
    }

    @SaCheckPermission("mall:popup-ad:update")
    @PutMapping("/{id}")
    @Operation(summary = "更新弹窗广告")
    public Result<Void> update(@PathVariable Long id, @RequestBody MallPopupAd popupAd) {
        popupAd.setId(id);
        // 状态由发布/下线操作管理，不允许编辑接口直接修改
        popupAd.setStatus(null);
        mallPopupAdService.updateById(popupAd);
        return Result.ok();
    }

    @SaCheckPermission("mall:popup-ad:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除弹窗广告")
    public Result<Void> delete(@PathVariable Long id) {
        mallPopupAdService.removeById(id);
        return Result.ok();
    }

    @SaCheckPermission("mall:popup-ad:publish")
    @PostMapping("/{id}/publish")
    @Operation(summary = "发布弹窗广告（草稿→投放中）")
    public Result<Void> publish(@PathVariable Long id) {
        mallPopupAdService.publish(id);
        return Result.ok();
    }

    @SaCheckPermission("mall:popup-ad:update")
    @PostMapping("/{id}/offline")
    @Operation(summary = "下线弹窗广告（投放中→已下架）")
    public Result<Void> offline(@PathVariable Long id) {
        mallPopupAdService.offline(id);
        return Result.ok();
    }
}
