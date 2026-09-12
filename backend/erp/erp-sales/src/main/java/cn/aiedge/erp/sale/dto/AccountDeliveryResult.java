package cn.aiedge.erp.sale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 账款交账 查询结果聚合
 * 按职员视图：staffList + summary；按单据视图：records + total + summary
 */
@Data
@Schema(description = "账款交账 查询结果")
public class AccountDeliveryResult {

    @Schema(description = "按单据视图：当前页记录")
    private List<AccountDeliveryDocVO> records = java.util.Collections.emptyList();

    @Schema(description = "按单据视图：总条数")
    private Long total = 0L;

    @Schema(description = "按职员视图：分组列表")
    private List<AccountDeliveryStaffVO> staffList = java.util.Collections.emptyList();

    @Schema(description = "五档统计卡片 + 合计")
    private AccountDeliverySummaryDTO summary = new AccountDeliverySummaryDTO();
}
