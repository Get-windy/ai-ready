package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应付账款VO
 */
@Data
@Schema(description = "应付账款详情")
public class PayableVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "应付编号")
    private String payableNo;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "原始金额")
    private BigDecimal originalAmount;

    @Schema(description = "已付金额")
    private BigDecimal paidAmount;

    @Schema(description = "剩余金额")
    private BigDecimal remainingAmount;

    @Schema(description = "开票日期")
    private LocalDate billDate;

    @Schema(description = "到期日期")
    private LocalDate dueDate;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "逾期天数")
    private Integer overdueDays;

    @Schema(description = "账龄区间")
    private String agingPeriod;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
