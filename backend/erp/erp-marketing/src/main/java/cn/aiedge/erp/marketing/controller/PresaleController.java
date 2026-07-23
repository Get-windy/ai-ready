package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.Presale;
import cn.aiedge.erp.marketing.service.PresaleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "预售活动管理")
@RestController
@RequestMapping("/api/erp/marketing/presale")
@RequiredArgsConstructor
public class PresaleController {

    private final PresaleService presaleService;

    @Operation(summary = "分页查询预售活动")
    @GetMapping("/page")
    public Result<IPage<Presale>> page(
            @RequestParam(required = false) String activityName,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        // 惰性结转：到期预售活动置为已结束
        presaleService.finishExpired();
        Page<Presale> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Presale> wrapper = new LambdaQueryWrapper<>();
        if (activityName != null && !activityName.isEmpty()) {
            wrapper.like(Presale::getActivityName, activityName);
        }
        if (status != null) {
            wrapper.eq(Presale::getStatus, status);
        }
        wrapper.orderByAsc(Presale::getSort).orderByDesc(Presale::getCreateTime);
        return Result.ok(presaleService.page(page, wrapper));
    }

    @Operation(summary = "查询预售活动详情")
    @GetMapping("/{id}")
    public Result<Presale> getById(@PathVariable Long id) {
        return Result.ok(presaleService.getById(id));
    }

    @Operation(summary = "创建预售活动")
    @PostMapping
    public Result<Boolean> create(@RequestBody Presale presale) {
        presale.setId(null);
        presale.setStatus(Presale.STATUS_PENDING);
        presale.setSoldCount(0);
        return Result.ok(presaleService.save(presale));
    }

    @Operation(summary = "更新预售活动")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Presale presale) {
        presale.setId(id);
        // sold_count 由参与记录累计，不允许手工维护
        presale.setSoldCount(null);
        return Result.ok(presaleService.updateById(presale));
    }

    @Operation(summary = "删除预售活动")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(presaleService.removeById(id));
    }

    @Operation(summary = "发布预售活动（校验时间与库存）")
    @PostMapping("/{id}/publish")
    public Result<Boolean> publish(@PathVariable Long id) {
        presaleService.publish(id);
        return Result.ok(true);
    }

    @Operation(summary = "取消预售活动")
    @PostMapping("/{id}/cancel")
    public Result<Boolean> cancel(@PathVariable Long id) {
        presaleService.cancel(id);
        return Result.ok(true);
    }
}
