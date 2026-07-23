package cn.aiedge.erp.finance.controller;

import cn.aiedge.base.log.annotation.OperationLog;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.finance.dto.CollectionStatsDTO;
import cn.aiedge.erp.finance.service.CollectionStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 回款统计Controller
 */
@Tag(name = "回款统计", description = "按日/周/月/业务员/客户维度统计回款")
@Slf4j
@RestController
@RequestMapping("/api/erp/finance/collection-stats")
@RequiredArgsConstructor
public class CollectionStatsController {

    private final CollectionStatsService collectionStatsService;

    @Operation(summary = "回款统计(汇总 + 分组明细)")
    @GetMapping
    @PreAuthorize("hasPermission('/api/erp/finance/collection-stats', 'finance:collection-stats:view')")
    @OperationLog(module = "回款统计", type = "QUERY", desc = "回款统计")
    public Result<CollectionStatsDTO> stats(
            @Parameter(description = "开始日期(收款日期, ISO格式)") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期(收款日期, ISO格式)") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "分组维度(day/week/month/staff/customer, 默认day)") @RequestParam(required = false) String groupBy) {
        return Result.success(collectionStatsService.stats(startDate, endDate, groupBy));
    }
}
