package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerGradeProductPrice;
import cn.aiedge.erp.stock.service.PartnerGradeProductPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "客户等级产品价格关联")
@RestController
@RequestMapping("/api/erp/pricing/partner-grade-prices")
@RequiredArgsConstructor
public class PartnerGradeProductPriceController {

    private final PartnerGradeProductPriceService partnerGradeProductPriceService;

    @Operation(summary = "查询客户等级的价格配置")
    @GetMapping("/{partnerGradeId}")
    public Result<List<PartnerGradeProductPrice>> getByGrade(@PathVariable Long partnerGradeId) {
        return Result.ok(partnerGradeProductPriceService.getByPartnerGradeId(partnerGradeId));
    }

    @Operation(summary = "批量保存客户等级价格配置")
    @PutMapping("/{partnerGradeId}")
    public Result<Boolean> batchSave(@PathVariable Long partnerGradeId, @RequestBody List<PartnerGradeProductPrice> list) {
        return Result.ok(partnerGradeProductPriceService.batchSave(partnerGradeId, list));
    }
}
