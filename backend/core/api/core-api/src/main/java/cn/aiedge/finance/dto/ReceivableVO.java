package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收账款VO
 */
@Data
@Schema(description = "应收账款详情")
public class ReceivableVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "应收编号")
    private String receivableNo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "原始金额")
    private BigDecimal originalAmount;

    @Schema(description = "已收金额")
    private BigDecimal receivedAmount;

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
