package cn.aiedge.statistics.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统计查询请求
 */
@Data
public class StatsQueryRequest {
    private Long tenantId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long deptId;
    private Long ownerId;
    private String period; // day, week, month, year
}
