package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.entity.CommissionRecord;
import cn.aiedge.erp.marketing.service.CommissionRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "佣金记录管理")
@RestController
@RequestMapping("/api/erp/marketing/commission/record")
@RequiredArgsConstructor
public class CommissionRecordController {

    private final CommissionRecordService commissionRecordService;

    @Operation(summary = "分页查询佣金记录")
    @GetMapping("/page")
    public Result<IPage<CommissionRecord>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<CommissionRecord> page = new Page<>(pageNum, pageSize);
        return Result.ok(commissionRecordService.page(page));
    }

    @Operation(summary = "获取佣金记录详情")
    @GetMapping("/{id}")
    public Result<CommissionRecord> getById(@PathVariable Long id) {
        return Result.ok(commissionRecordService.getById(id));
    }

    @Operation(summary = "删除佣金记录")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(commissionRecordService.removeById(id));
    }
}
