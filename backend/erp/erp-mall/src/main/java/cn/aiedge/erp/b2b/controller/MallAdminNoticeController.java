package cn.aiedge.erp.b2b.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.b2b.model.MallNotice;
import cn.aiedge.erp.b2b.service.MallNoticeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 商城公告管理（管理后台）
 */
@RestController
@RequestMapping("/api/erp/mall/admin/notice")
@Tag(name = "商城公告管理", description = "商城公告的分页/创建/发布/下线等管理接口")
@RequiredArgsConstructor
public class MallAdminNoticeController {

    private final MallNoticeService mallNoticeService;

    @SaCheckPermission("mall:notice:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询公告")
    public Result<IPage<MallNotice>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "标题关键词") @RequestParam(required = false) String title,
            @Parameter(description = "公告类型 1公告 2活动 3系统") @RequestParam(required = false) Integer noticeType,
            @Parameter(description = "状态 0草稿 1已发布 2已下线") @RequestParam(required = false) Integer status) {
        return Result.ok(mallNoticeService.pageNotices(pageNum, pageSize, title, noticeType, status));
    }

    @SaCheckPermission("mall:notice:detail")
    @GetMapping("/{id}")
    @Operation(summary = "查询公告详情")
    public Result<MallNotice> getById(@PathVariable Long id) {
        return Result.ok(mallNoticeService.getNotice(id));
    }

    @SaCheckPermission("mall:notice:create")
    @PostMapping
    @Operation(summary = "创建公告")
    public Result<Void> create(@RequestBody MallNotice notice) {
        mallNoticeService.createNotice(notice);
        return Result.ok();
    }

    @SaCheckPermission("mall:notice:update")
    @PutMapping("/{id}")
    @Operation(summary = "更新公告")
    public Result<Void> update(@PathVariable Long id, @RequestBody MallNotice notice) {
        notice.setId(id);
        mallNoticeService.updateNotice(notice);
        return Result.ok();
    }

    @SaCheckPermission("mall:notice:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告")
    public Result<Void> delete(@PathVariable Long id) {
        mallNoticeService.deleteNotice(id);
        return Result.ok();
    }

    @SaCheckPermission("mall:notice:publish")
    @PutMapping("/{id}/publish")
    @Operation(summary = "发布公告")
    public Result<Void> publish(@PathVariable Long id) {
        mallNoticeService.publishNotice(id);
        return Result.ok();
    }

    @SaCheckPermission("mall:notice:update")
    @PutMapping("/{id}/offline")
    @Operation(summary = "下线公告")
    public Result<Void> offline(@PathVariable Long id) {
        mallNoticeService.offlineNotice(id);
        return Result.ok();
    }
}
