package cn.aiedge.erp.supplier.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 日期范围
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateRange {
    
    private LocalDateTime start;
    private LocalDateTime end;
}
