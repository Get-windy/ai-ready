package cn.aiedge.erp.b2b.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.b2b.model.MallNotice;
import cn.aiedge.erp.b2b.service.MallNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城公告（商城端公开查询）
 * 仅返回已发布公告；免登录权限按模块现状（与商城商品浏览一致，走全局 Sa-Token 拦截器现状配置）
 */
@RestController
@RequestMapping("/api/erp/mall/notice")
@Tag(name = "商城公告(商城端)", description = "商城端已发布公告公开查询")
@RequiredArgsConstructor
public class MallNoticeController {

    private final MallNoticeService mallNoticeService;

    @GetMapping("/list")
    @Operation(summary = "查询已发布公告列表")
    public Result<List<MallNotice>> list(
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(mallNoticeService.listPublished(limit));
    }
}
