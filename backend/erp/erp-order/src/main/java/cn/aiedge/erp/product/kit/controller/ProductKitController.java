package cn.aiedge.erp.product.kit.controller;

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

@Slf4j
@RestController
@RequestMapping("/api/erp/product-kit")
@RequiredArgsConstructor
@Tag(name = "商品套装管理", description = "套装定义、组件管理、成本计算等操作")
public class ProductKitController {

    private final ProductKitService productKitService;

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

    @GetMapping("/{id}/items")
    @Operation(summary = "获取套装组件")
    public List<ProductKitItem> getKitItems(@PathVariable Long id) {
        return productKitService.getKitItems(id);
    }

    @GetMapping("/active")
    @Operation(summary = "获取活跃套装列表")
    public List<ProductKitVO> listActiveKits() {
        return productKitService.listActiveKits().stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

    @GetMapping("/type/{kitType}")
    @Operation(summary = "按类型获取套装列表")
    public List<ProductKitVO> listByKitType(@PathVariable Integer kitType) {
        return productKitService.listByKitType(kitType).stream()
                .map(this::convertToVO).collect(Collectors.toList());
    }

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

    @PostMapping("/{id}/copy")
    @Operation(summary = "复制套装")
    public ProductKitVO copy(@PathVariable Long id) {
        ProductKit kit = productKitService.copyKit(id);
        return convertToVO(kit);
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "激活套装")
    public void activate(@PathVariable Long id) {
        productKitService.activateKit(id);
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "停用套装")
    public void deactivate(@PathVariable Long id) {
        productKitService.deactivateKit(id);
    }

    @PostMapping("/{id}/items")
    @Operation(summary = "添加套装组件")
    public ProductKitItem addKitItem(@PathVariable Long id, @RequestBody ProductKitItemDTO dto) {
        ProductKitItem item = new ProductKitItem();
        BeanUtils.copyProperties(dto, item);
        return productKitService.addKitItem(id, item);
    }

    @PutMapping("/{id}/items/{itemId}")
    @Operation(summary = "更新套装组件")
    public ProductKitItem updateKitItem(@PathVariable Long itemId, @RequestBody ProductKitItemDTO dto) {
        ProductKitItem item = new ProductKitItem();
        BeanUtils.copyProperties(dto, item);
        return productKitService.updateKitItem(itemId, item);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @Operation(summary = "删除套装组件")
    public void removeKitItem(@PathVariable Long itemId) {
        productKitService.removeKitItem(itemId);
    }

    @GetMapping("/{id}/check-availability")
    @Operation(summary = "检查套装库存可用性")
    public Boolean checkAvailability(
            @PathVariable Long id,
            @RequestParam Long warehouseId,
            @RequestParam BigDecimal quantity) {
        return productKitService.checkKitAvailability(id, warehouseId, quantity);
    }

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