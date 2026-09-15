package cn.aiedge.trade.monitor.dto;

import java.time.LocalDateTime;

/**
 * 调用日志查询条件（页面「接口调用日志」Tab）
 *
 * @param pageNum     页码（从 1 起）
 * @param pageSize    每页条数
 * @param channelCode 渠道编码（精确）
 * @param apiPath     接口路径（模糊）
 * @param direction   方向（IN / OUT / SANDBOX，空=全部）
 * @param status      状态（SUCCESS / FAIL，空=全部）
 * @param keyword     关键字（request_id / 错误信息 模糊）
 * @param startTime   开始时间
 * @param endTime     结束时间
 */
public record ApiCallQuery(
        long pageNum,
        long pageSize,
        String channelCode,
        String apiPath,
        String direction,
        String status,
        String keyword,
        LocalDateTime startTime,
        LocalDateTime endTime) {
}
