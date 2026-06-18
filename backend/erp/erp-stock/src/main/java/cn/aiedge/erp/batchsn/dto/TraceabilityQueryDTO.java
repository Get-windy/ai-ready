package cn.aiedge.erp.batchsn.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 追溯查询DTO
 *
 * @author team-member
 * @date 2026-04-29
 */
@Data
public class TraceabilityQueryDTO {

    private String traceType;
    private String traceCode;
    private String queryType;
    private LocalDateTime timeRangeStart;
    private LocalDateTime timeRangeEnd;
}
