package cn.aiedge.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款更新请求
 */
@Data
@Schema(description = "应收账款更新请求")
public class ReceivableUpdateRequest {

    @Schema(description = "ID", required = true)
    private Long id;

    @Schema(description = "备注")
    private String remark;
}
