package cn.aiedge.dms.orderpool.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 抢单请求
 */
@Data
public class GrabRequest {

    @NotNull(message = "骑手ID不能为空")
    private Long riderId;

    private String riderName;
}
