package cn.aiedge.erp.marketing.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.marketing.dto.StaffCommissionSummaryDTO;
import cn.aiedge.erp.marketing.service.CommissionRecordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工提成汇总控制器
 */
@Slf4j
@Tag(name = "员工提成汇总")
@RestController
@RequestMapping("/api/erp/marketing/commission")
@RequiredArgsConstructor
public class CommissionStaffSummaryController {

    private final CommissionRecordService commissionRecordService;

    @Operation(summary = "员工提成汇总分页",
            description = "按推荐人(referrerId=员工/业务员)聚合: 成单数、订单金额合计、提成金额合计、按结算状态拆分(未结DRAFT/CONFIRMED、已结PAID)")
    @GetMapping("/staff-summary/page")
    public Result<Page<StaffCommissionSummaryDTO>> staffSummaryPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        return Result.ok(commissionRecordService.pageStaffSummary(page, size, startDate, endDate, keyword));
    }
}
