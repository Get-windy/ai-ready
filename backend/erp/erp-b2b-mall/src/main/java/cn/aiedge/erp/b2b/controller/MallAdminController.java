package cn.aiedge.erp.b2b.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.b2b.model.MallOrder;
import cn.aiedge.erp.b2b.model.MallProduct;
import cn.aiedge.erp.b2b.model.ShopBanner;
import cn.aiedge.erp.b2b.model.ShopConfig;
import cn.aiedge.erp.b2b.model.ShopTemplate;
import cn.aiedge.erp.b2b.model.ShopUser;
import cn.aiedge.erp.b2b.service.MallAdminService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/erp/mall/admin")
@Tag(name = "商城管理后台", description = "商城配置、用户审核、轮播图管理等管理接口")
@RequiredArgsConstructor
public class MallAdminController {

    private final MallAdminService mallAdminService;

    // ==================== 商城配置 ====================

    @GetMapping("/config")
    @Operation(summary = "获取商城配置")
    public Result<ShopConfig> getConfig() {
        return Result.ok(mallAdminService.getConfig());
    }

    @PutMapping("/config")
    @Operation(summary = "更新商城配置")
    public Result<Void> updateConfig(@RequestBody ShopConfig config) {
        mallAdminService.updateConfig(config);
        return Result.ok();
    }

    // ==================== 商城用户审核 ====================

    @GetMapping("/user/page")
    @Operation(summary = "分页查询商城用户")
    public Result<Page<ShopUser>> pageUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer pageSize,
            @Parameter(description = "关键词(用户名/昵称/手机号/公司)") @RequestParam(required = false) String keyword,
            @Parameter(description = "审核状态 0待审 1通过 2驳回") @RequestParam(required = false) Integer auditStatus,
            @Parameter(description = "状态 1正常 0禁用") @RequestParam(required = false) Integer status) {
        return Result.ok(mallAdminService.pageUsers(pageNum, pageSize, keyword, auditStatus, status));
    }

    @PutMapping("/user/{id}/approve")
    @Operation(summary = "审核通过")
    public Result<Void> approveUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        mallAdminService.approveUser(id);
        return Result.ok();
    }

    @PutMapping("/user/{id}/reject")
    @Operation(summary = "审核驳回")
    public Result<Void> rejectUser(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "驳回原因") @RequestParam String reason) {
        mallAdminService.rejectUser(id, reason);
        return Result.ok();
    }

    @PutMapping("/user/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public Result<Void> toggleUserStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态 1正常 0禁用") @RequestParam Integer status) {
        mallAdminService.toggleUserStatus(id, status);
        return Result.ok();
    }

    // ==================== 轮播图管理 ====================

    @GetMapping("/banner")
    @Operation(summary = "获取轮播图列表")
    public Result<List<ShopBanner>> listBanners() {
        return Result.ok(mallAdminService.listBanners());
    }

    @PostMapping("/banner")
    @Operation(summary = "创建轮播图")
    public Result<Void> createBanner(@RequestBody ShopBanner banner) {
        mallAdminService.createBanner(banner);
        return Result.ok();
    }

    @PutMapping("/banner/{id}")
    @Operation(summary = "更新轮播图")
    public Result<Void> updateBanner(
            @Parameter(description = "轮播图ID") @PathVariable Long id,
            @RequestBody ShopBanner banner) {
        banner.setId(id);
        mallAdminService.updateBanner(banner);
        return Result.ok();
    }

    @DeleteMapping("/banner/{id}")
    @Operation(summary = "删除轮播图")
    public Result<Void> deleteBanner(@Parameter(description = "轮播图ID") @PathVariable Long id) {
        mallAdminService.deleteBanner(id);
        return Result.ok();
    }

    // ==================== 页面模板 ====================

    @GetMapping("/template/list")
    @Operation(summary = "获取启用的模板列表")
    public Result<List<ShopTemplate>> listTemplates() {
        return Result.ok(mallAdminService.listTemplates());
    }

    // ==================== 商品管理 ====================

    @GetMapping("/product/page")
    @Operation(summary = "分页查询商城商品")
    public Result<Page<MallProduct>> pageProducts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status) {
        return Result.ok(mallAdminService.pageProducts(pageNum, pageSize, keyword, categoryId, status));
    }

    @PostMapping("/product")
    @Operation(summary = "创建商品")
    public Result<Void> createProduct(@RequestBody MallProduct product) {
        mallAdminService.saveProduct(product);
        return Result.ok();
    }

    @PutMapping("/product/{id}")
    @Operation(summary = "更新商品")
    public Result<Void> updateProduct(
            @PathVariable Long id,
            @RequestBody MallProduct product) {
        product.setId(id);
        mallAdminService.saveProduct(product);
        return Result.ok();
    }

    @DeleteMapping("/product/{id}")
    @Operation(summary = "删除商品")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        mallAdminService.deleteProduct(id);
        return Result.ok();
    }

    // ==================== 订单管理 ====================

    @GetMapping("/order/page")
    @Operation(summary = "分页查询商城订单")
    public Result<Page<MallOrder>> pageOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String orderStatus) {
        return Result.ok(mallAdminService.pageOrders(pageNum, pageSize, keyword, orderStatus));
    }

    @GetMapping("/order/{id}")
    @Operation(summary = "获取订单详情")
    public Result<MallOrder> getOrderDetail(@PathVariable Long id) {
        return Result.ok(mallAdminService.getOrderDetail(id));
    }

    @PutMapping("/order/{id}/approve")
    @Operation(summary = "审核通过订单")
    public Result<Void> approveOrder(@PathVariable Long id) {
        mallAdminService.approveOrder(id);
        return Result.ok();
    }

    @PutMapping("/order/{id}/reject")
    @Operation(summary = "审核驳回订单")
    public Result<Void> rejectOrder(
            @PathVariable Long id,
            @RequestParam String reason) {
        mallAdminService.rejectOrder(id, reason);
        return Result.ok();
    }
}
