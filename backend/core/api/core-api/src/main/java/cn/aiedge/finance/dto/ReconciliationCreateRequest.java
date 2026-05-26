package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 对账记录创建请求
 */
@Data
@Schema(description = "对账记录创建请求")
public class ReconciliationCreateRequest {

    @Schema(description = "对账类型", required = true)
    @NotBlank(message = "对账类型不能为空")
    private String reconciliationType; // BANK-银行 CUSTOMER-客户 SUPPLIER-供应商

    @Schema(description = "目标ID", required = true)
    @NotNull(message = "目标ID不能为空")
    private Long targetId; // 对方ID

    @Schema(description = "目标名称")
    private String targetName;

    @Schema(description = "开始日期", required = true)
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", required = true)
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "系统余额", required = true)
    @NotNull(message = "系统余额不能为空")
    @DecimalMin(value = "0.00", message = "系统余额不能小于0")
    private BigDecimal systemBalance;

    @Schema(description = "实际余额", required = true)
    @NotNull(message = "实际余额不能为空")
    @DecimalMin(value = "0.00", message = "实际余额不能小于0")
    private BigDecimal actualBalance;

    @Schema(description = "对账明细项")
    private List<ReconciliationItemCreateRequest> items;

    @Schema(description = "备注")
    private String remark;
}
