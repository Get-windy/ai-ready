package cn.aiedge.dms.orderpool.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 取消竞价请求
 */
@Data
public class CancelBidRequest {

    @NotNull(message = "竞价记录ID不能为空")
    private Long bidId;

    @NotNull(message = "骑手ID不能为空")
    private Long riderId;
}
