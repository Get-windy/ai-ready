package cn.aiedge.dms.orderpool.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 竞价请求
 */
@Data
public class BidRequest {

    @NotNull(message = "骑手ID不能为空")
    private Long riderId;

    private String riderName;

    @NotNull(message = "出价不能为空")
    private BigDecimal price;
}
