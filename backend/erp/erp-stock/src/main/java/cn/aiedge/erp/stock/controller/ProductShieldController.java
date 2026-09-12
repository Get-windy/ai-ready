package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductShield;
import cn.aiedge.erp.stock.service.ProductShieldService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品授权（屏蔽客户）Controller —— 商品列表「商品授权」子标签
 */
@Slf4j
@Tag(name = "商品授权")
@RestController
@RequestMapping("/api/erp/product-shield")
@RequiredArgsConstructor
public class ProductShieldController {

    private final ProductShieldService productShieldService;

    @Operation(summary = "分页查询商品授权")
    @SaCheckPermission("erp:product:list")
    @GetMapping("/page")
    public Result<IPage<ProductShield>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String shieldLevel,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Long partnerId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productShieldService.getShieldPage(keyword, shieldLevel, region, partnerId, pageNum, pageSize));
    }

    @Operation(summary = "保存/更新授权记录")
    @SaCheckPermission("erp:product:update")
    @PostMapping
    public Result<Boolean> save(@RequestBody ProductShield shield) {
        if (shield.getShieldLevel() == null || shield.getShieldLevel().isEmpty()) {
            shield.setShieldLevel("ALL");
        }
        return Result.ok(productShieldService.save(shield));
    }

    @Operation(summary = "批量屏蔽")
    @SaCheckPermission("erp:product:update")
    @PostMapping("/batch-shield")
    public Result<Integer> batchShield(@RequestBody Map<String, Object> body) {
        List<Long> productIds = toLongList(body.get("productIds"));
        List<Long> partnerIds = toLongList(body.get("partnerIds"));
        String partnerNames = body.get("partnerNames") == null ? null : String.valueOf(body.get("partnerNames"));
        String shieldLevel = body.get("shieldLevel") == null ? "ALL" : String.valueOf(body.get("shieldLevel"));
        String region = body.get("region") == null ? null : String.valueOf(body.get("region"));
        return Result.ok(productShieldService.batchShield(productIds, partnerIds, partnerNames, shieldLevel, region));
    }

    @Operation(summary = "批量取消授权")
    @SaCheckPermission("erp:product:update")
    @PostMapping("/batch-cancel")
    public Result<Integer> batchCancel(@RequestBody Map<String, Object> body) {
        return Result.ok(productShieldService.batchCancel(toLongList(body.get("ids"))));
    }

    @SuppressWarnings("unchecked")
    private List<Long> toLongList(Object raw) {
        if (raw == null) {
            return null;
        }
        List<Long> list = new java.util.ArrayList<>();
        if (raw instanceof List<?>) {
            for (Object o : (List<Object>) raw) {
                if (o == null) {
                    continue;
                }
                if (o instanceof Number) {
                    list.add(((Number) o).longValue());
                } else {
                    String s = String.valueOf(o).trim();
                    if (!s.isEmpty()) {
                        list.add(Long.valueOf(s));
                    }
                }
            }
        }
        return list;
    }
}
