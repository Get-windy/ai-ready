package cn.aiedge.erp.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "往来对冲DTO")
public class OffsetDTO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "对冲单号")
    private String offsetNo;

    @Schema(description = "对方类型: customer/supplier")
    private String partyType;

    @Schema(description = "对方ID")
    private Long partyId;

    @Schema(description = "对方名称")
    private String partyName;

    @Schema(description = "应收金额")
    private BigDecimal receivableAmount;

    @Schema(description = "应付金额")
    private BigDecimal payableAmount;

    @Schema(description = "对冲金额")
    private BigDecimal offsetAmount;

    @Schema(description = "差额")
    private BigDecimal balanceAmount;

    @Schema(description = "对冲日期")
    private LocalDate offsetDate;

    @Schema(description = "状态: draft/completed/cancelled")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "对冲明细")
    private List<OffsetItemDTO> items;

    @Schema(description = "创建时间")
    private java.time.LocalDateTime createTime;
}
