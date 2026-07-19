package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.LoyaltyCard;
import cn.aiedge.erp.marketing.service.LoyaltyCardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Tag(name = "会员卡管理")
@RestController
@RequestMapping("/api/erp/marketing/card")
@RequiredArgsConstructor
public class LoyaltyCardController {

    private final LoyaltyCardService loyaltyCardService;

    @Operation(summary = "分页查询会员卡")
    @GetMapping("/page")
    public Result<IPage<LoyaltyCard>> page(
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<LoyaltyCard> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<LoyaltyCard> wrapper = new LambdaQueryWrapper<>();
        if (memberId != null) {
            wrapper.eq(LoyaltyCard::getPartnerId, memberId);
        }
        return Result.ok(loyaltyCardService.page(page, wrapper));
    }

    @Operation(summary = "查询会员的所有卡片")
    @GetMapping("/member/{memberId}")
    public Result<List<LoyaltyCard>> listByMember(@PathVariable Long memberId) {
        return Result.ok(loyaltyCardService.listByMember(memberId));
    }

    @Operation(summary = "获取卡片详情")
    @GetMapping("/{id}")
    public Result<LoyaltyCard> getById(@PathVariable Long id) {
        LoyaltyCard card = loyaltyCardService.getById(id);
        if (card == null) {
            return Result.fail("卡片不存在");
        }
        return Result.ok(card);
    }

    @Operation(summary = "创建会员卡")
    @PostMapping
    public Result<Boolean> create(@RequestBody LoyaltyCard card) {
        if (card.getPoints() == null) card.setPoints(BigDecimal.ZERO);
        if (card.getIsActive() == null) card.setIsActive(1);
        return Result.ok(loyaltyCardService.save(card));
    }

    @Operation(summary = "更新会员卡")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody LoyaltyCard card) {
        card.setId(id);
        return Result.ok(loyaltyCardService.updateById(card));
    }

    @Operation(summary = "增加积分")
    @PostMapping("/{id}/points/add")
    public Result<Boolean> addPoints(@PathVariable Long id, @RequestParam Integer points) {
        loyaltyCardService.addPoints(id, points);
        return Result.ok(true);
    }

    @Operation(summary = "扣减积分")
    @PostMapping("/{id}/points/deduct")
    public Result<Boolean> deductPoints(@PathVariable Long id, @RequestParam Integer points) {
        loyaltyCardService.deductPoints(id, points);
        return Result.ok(true);
    }

    @Operation(summary = "删除会员卡")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(loyaltyCardService.removeById(id));
    }
}
