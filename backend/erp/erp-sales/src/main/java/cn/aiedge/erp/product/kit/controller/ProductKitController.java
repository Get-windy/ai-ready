package cn.aiedge.erp.product.kit.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.product.kit.dto.ProductKitCreateDTO;
import cn.aiedge.erp.product.kit.dto.ProductKitItemDTO;
import cn.aiedge.erp.product.kit.dto.ProductKitVO;
import cn.aiedge.erp.product.kit.entity.ProductKit;
import cn.aiedge.erp.product.kit.entity.ProductKitItem;
import cn.aiedge.erp.product.kit.service.ProductKitService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@RestController
@RequestMapping("/api/erp/product-kit")
@RequiredArgsConstructor
@Tag(name = "商品套装管理", description = "套装定义、组件管理、成本计算等操作")
public class ProductKitController {

    private final ProductKitService productKitService;

    @SaCheckPermission("product:kit:list")
    @GetMapping("/page")
    @Operation(summary = "分页查询套装")
    public Page<ProductKitVO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "套装类型") @RequestParam(required = false) Integer kitType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<ProductKit> page = productKitService.pageList(keyword, kitType, status, pageNum, pageSize);
        Page<ProductKitVO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        return voPage;
    }

    @SaCheckPermission("product:kit:detail")
    @GetMapping("/{id}")
    @Operation(summary = "获取套装详情")
    public ProductKitVO getById(@PathVariable Long id) {
        ProductKit kit = productKitService.getById(id);
        if (kit == null) {
            throw new RuntimeException("套装不存在");
        }
        ProductKitVO vo = convertToVO(kit);
        vo.setItems(productKitService.getKitItems(id));
        return vo;
    }

    @SaCheckPermission("product:kit:list")
    @GetMapping("/{id}/items")
    @Operation(summary = "获取套装组件")
    public List<ProductKitItem> getKitItems(@PathVariable Long id) {
        return productKitService.getKitItems(id);
    }

    @SaCheckPermission("product:kit:view")
    @GetMapping("/active")
    @Operation(summary = "获取活跃套装列表")
    public List<ProductKitVO> listActiveKits() {
        return productKitService.listActiveKits().stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("product:kit:detail")
    @GetMapping("/type/{kitType}")
    @Operation(summary = "按类型获取套装列表")
    public List<ProductKitVO> listByKitType(@PathVariable Integer kitType) {
        return productKitService.listByKitType(kitType).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @SaCheckPermission("product:kit:create")
    @PostMapping
    @Operation(summary = "创建套装")
    public ProductKitVO create(@RequestBody ProductKitCreateDTO dto) {
        ProductKit kit = new ProductKit();
        BeanUtils.copyProperties(dto, kit);
        kit.setTenantId(1L);
        kit.setCreateBy(StpUtil.getLoginIdAsLong());
        List<ProductKitItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                ProductKitItem item = new ProductKitItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        ProductKit created = productKitService.createKit(kit, items);
        return convertToVO(created);
    }

    @SaCheckPermission("product:kit:update")
    @PutMapping("/{id}")
    @Operation(summary = "更新套装")
    public ProductKitVO update(@PathVariable Long id, @RequestBody ProductKitCreateDTO dto) {
        ProductKit kit = new ProductKit();
        BeanUtils.copyProperties(dto, kit);
        List<ProductKitItem> items = null;
        if (dto.getItems() != null) {
            items = dto.getItems().stream().map(itemDTO -> {
                ProductKitItem item = new ProductKitItem();
                BeanUtils.copyProperties(itemDTO, item);
                return item;
            }).collect(Collectors.toList());
        }
        ProductKit updated = productKitService.updateKit(id, kit, items);
        return convertToVO(updated);
    }

    @SaCheckPermission("product:kit:create")
    @PostMapping("/{id}/copy")
    @Operation(summary = "复制套装")
    public ProductKitVO copy(@PathVariable Long id) {
        ProductKit kit = productKitService.copyKit(id);
        return convertToVO(kit);
    }

    @SaCheckPermission("product:kit:update")
    @PostMapping("/{id}/activate")
    @Operation(summary = "激活套装")
    public void activate(@PathVariable Long id) {
        productKitService.activateKit(id);
    }

    @SaCheckPermission("product:kit:update")
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "停用套装")
    public void deactivate(@PathVariable Long id) {
        productKitService.deactivateKit(id);
    }

    @SaCheckPermission("product:kit:update")
    @PostMapping("/batch-activate")
    @Operation(summary = "批量激活套装", description = "请求体 {\"ids\":[1,2]}；返回实际更新条数")
    public Result<Integer> batchActivate(@RequestBody Map<String, List<Long>> body) {
        return Result.ok(productKitService.activateKits(body == null ? null : body.get("ids")));
    }

    @SaCheckPermission("product:kit:update")
    @PostMapping("/batch-deactivate")
    @Operation(summary = "批量停用套装", description = "请求体 {\"ids\":[1,2]}；返回实际更新条数")
    public Result<Integer> batchDeactivate(@RequestBody Map<String, List<Long>> body) {
        return Result.ok(productKitService.deactivateKits(body == null ? null : body.get("ids")));
    }

    @SaCheckPermission("product:kit:update")
    @PostMapping("/{id}/items")
    @Operation(summary = "添加套装组件")
    public ProductKitItem addKitItem(@PathVariable Long id, @RequestBody ProductKitItemDTO dto) {
        ProductKitItem item = new ProductKitItem();
        BeanUtils.copyProperties(dto, item);
        return productKitService.addKitItem(id, item);
    }

    @SaCheckPermission("product:kit:update")
    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新套装组件")
    public ProductKitItem updateKitItem(@PathVariable Long itemId, @RequestBody ProductKitItemDTO dto) {
        ProductKitItem item = new ProductKitItem();
        BeanUtils.copyProperties(dto, item);
        return productKitService.updateKitItem(itemId, item);
    }

    @SaCheckPermission("product:kit:delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除套装", description = "逻辑删除套装并级联逻辑删除其组件行")
    public Result<Boolean> delete(@PathVariable Long id) {
        productKitService.deleteKit(id);
        return Result.ok(true);
    }

    @SaCheckPermission("product:kit:delete")
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除套装", description = "请求体 {\"ids\":[1,2]}；级联逻辑删除组件行，返回实际删除条数")
    public Result<Integer> batchDelete(@RequestBody Map<String, List<Long>> body) {
        return Result.ok(productKitService.deleteKits(body == null ? null : body.get("ids")));
    }

    @SaCheckPermission("product:kit:delete")
    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除套装组件")
    public void removeKitItem(@PathVariable Long itemId) {
        productKitService.removeKitItem(itemId);
    }

    @SaCheckPermission("product:kit:view")
    @GetMapping("/{id}/check-availability")
    @Operation(summary = "检查套装库存可用性")
    public Boolean checkAvailability(
            @PathVariable Long id,
            @RequestParam Long warehouseId,
            @RequestParam BigDecimal quantity) {
        return productKitService.checkKitAvailability(id, warehouseId, quantity);
    }

    @SaCheckPermission("product:kit:view")
    @GetMapping("/items-summary")
    @Operation(summary = "批量查询套装商品明细摘要（商品列表「套餐」子标签用）")
    public Map<Long, String> itemsSummary(@RequestParam("kitIds") List<Long> kitIds) {
        return productKitService.itemsSummary(kitIds);
    }

    @SaCheckPermission("product:kit:view")
    @GetMapping("/statistics")
    @Operation(summary = "套装统计")
    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeKits", productKitService.lambdaQuery()
                .eq(ProductKit::getDeleted, 0)
                .eq(ProductKit::getStatus, 1)
                .count());
        stats.put("totalKits", productKitService.lambdaQuery()
                .eq(ProductKit::getDeleted, 0)
                .count());
        return stats;
    }

    private ProductKitVO convertToVO(ProductKit kit) {
        ProductKitVO vo = new ProductKitVO();
        BeanUtils.copyProperties(kit, vo);
        return vo;
    }
}