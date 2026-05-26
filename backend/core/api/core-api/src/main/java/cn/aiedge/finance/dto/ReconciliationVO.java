package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对账记录VO
 */
@Data
@Schema(description = "对账记录详情")
public class ReconciliationVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "对账编号")
    private String reconciliationNo;

    @Schema(description = "对账类型")
    private String reconciliationType; // BANK-银行 CUSTOMER-客户 SUPPLIER-供应商

    @Schema(description = "目标ID")
    private Long targetId; // 对方ID

    @Schema(description = "目标名称")
    private String targetName;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "对账日期")
    private LocalDate reconciliationDate;

    @Schema(description = "系统余额")
    private BigDecimal systemBalance; // 系统余额

    @Schema(description = "实际余额")
    private BigDecimal actualBalance; // 实际余额

    @Schema(description = "差异")
    private BigDecimal difference; // 差异

    @Schema(description = "状态")
    private Integer status; // 0-待对账 1-已对账 2-有差异

    @Schema(description = "差异原因")
    private String differenceReason;

    @Schema(description = "处理人ID")
    private String handlerId;

    @Schema(description = "处理人姓名")
    private String handlerName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "对账明细项列表")
    private List<ReconciliationItemVO> items;
}
