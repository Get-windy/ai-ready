package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

/**
 * 对账记录更新请求
 */
@Data
@Schema(description = "对账记录更新请求")
public class ReconciliationUpdateRequest {

    @Schema(description = "ID", required = true)
    private Long id;

    @Schema(description = "差异原因")
    private String differenceReason;

    @Schema(description = "处理人ID")
    private String handlerId;

    @Schema(description = "处理人姓名")
    private String handlerName;

    @Schema(description = "备注")
    private String remark;
}
