package cn.aiedge.erp.finance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会计期间DTO
 */
@Data
public class AccountingPeriodDTO {

    private Long id;

    @NotNull(message = "会计年度不能为空")
    private Integer periodYear;

    @NotNull(message = "会计月份不能为空")
    @Min(value = 1, message = "会计月份必须在1-12之间")
    @Max(value = 12, message = "会计月份必须在1-12之间")
    private Integer periodMonth;

    /**
     * 期间编码，格式 yyyy-MM，如 2026-07（不传则按年度+月份生成）
     */
    private String periodCode;

    private LocalDate startDate;

    private LocalDate endDate;

    /**
     * 状态：1-开启 0-关闭（已月结）
     */
    private Integer status;

    private String closedBy;

    private LocalDateTime closedTime;

    private String remark;
}
