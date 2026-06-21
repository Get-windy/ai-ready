package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerLogisticsExt;
import cn.aiedge.erp.stock.service.PartnerLogisticsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "物流公司扩展管理")
@RestController
@RequestMapping("/api/erp/partner/logistics")
@RequiredArgsConstructor
public class PartnerLogisticsController {

    private final PartnerLogisticsService partnerLogisticsService;

    @Operation(summary = "分页查询物流公司扩展")
    @GetMapping("/page")
    public Result<IPage<PartnerLogisticsExt>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(partnerLogisticsService.getPage(keyword, pageNum, pageSize));
    }

    @Operation(summary = "查询物流公司扩展详情")
    @GetMapping("/{partnerId}")
    public Result<PartnerLogisticsExt> getByPartnerId(@PathVariable Long partnerId) {
        return Result.ok(partnerLogisticsService.getByPartnerId(partnerId));
    }

    @Operation(summary = "创建物流公司扩展")
    @PostMapping
    public Result<Boolean> create(@RequestBody PartnerLogisticsExt ext) {
        return Result.ok(partnerLogisticsService.createLogisticsExt(ext));
    }

    @Operation(summary = "更新物流公司扩展")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody PartnerLogisticsExt ext) {
        return Result.ok(partnerLogisticsService.updateLogisticsExt(id, ext));
    }
}
