package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.FlashSale;
import cn.aiedge.erp.marketing.entity.FlashSaleOrder;
import cn.aiedge.erp.marketing.service.FlashSaleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Tag(name = "秒杀场次管理")
@RestController
@RequestMapping("/api/erp/marketing/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @Operation(summary = "分页查询秒杀场次")
    @SaCheckPermission("marketing:flash-sale:list")
    @GetMapping("/page")
    public Result<IPage<FlashSale>> page(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        // 惰性结转：到期场次置为已结束
        flashSaleService.finishExpired();
        Page<FlashSale> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FlashSale> wrapper = new LambdaQueryWrapper<>();
        if (title != null && !title.isEmpty()) {
            wrapper.like(FlashSale::getTitle, title);
        }
        if (status != null) {
            wrapper.eq(FlashSale::getStatus, status);
        }
        wrapper.orderByAsc(FlashSale::getSort).orderByDesc(FlashSale::getCreateTime);
        return Result.ok(flashSaleService.page(page, wrapper));
    }

    @Operation(summary = "查询秒杀场次详情")
    @SaCheckPermission("marketing:flash-sale:detail")
    @GetMapping("/{id}")
    public Result<FlashSale> getById(@PathVariable Long id) {
        return Result.ok(flashSaleService.getById(id));
    }

    @Operation(summary = "创建秒杀场次")
    @SaCheckPermission("marketing:flash-sale:create")
    @PostMapping
    public Result<Boolean> create(@RequestBody FlashSale flashSale) {
        flashSale.setId(null);
        flashSale.setStatus(FlashSale.STATUS_DRAFT);
        flashSale.setSoldCount(0);
        return Result.ok(flashSaleService.save(flashSale));
    }

    @Operation(summary = "更新秒杀场次")
    @SaCheckPermission("marketing:flash-sale:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody FlashSale flashSale) {
        flashSale.setId(id);
        // sold_count 由参与记录累计，不允许手工维护
        flashSale.setSoldCount(null);
        return Result.ok(flashSaleService.updateById(flashSale));
    }

    @Operation(summary = "删除秒杀场次")
    @SaCheckPermission("marketing:flash-sale:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(flashSaleService.removeById(id));
    }

    @Operation(summary = "发布秒杀场次（校验时间与库存）")
    @SaCheckPermission("marketing:flash-sale:publish")
    @PostMapping("/{id}/publish")
    public Result<Boolean> publish(@PathVariable Long id) {
        flashSaleService.publish(id);
        return Result.ok(true);
    }

    @Operation(summary = "取消秒杀场次")
    @SaCheckPermission("marketing:flash-sale:create")
    @PostMapping("/{id}/cancel")
    public Result<Boolean> cancel(@PathVariable Long id) {
        flashSaleService.cancel(id);
        return Result.ok(true);
    }

    @Operation(summary = "分页查询场次参与记录")
    @SaCheckPermission("marketing:flash-sale:view")
    @GetMapping("/{id}/participants")
    public Result<IPage<FlashSaleOrder>> participants(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(flashSaleService.pageParticipants(id, pageNum, pageSize));
    }
}
