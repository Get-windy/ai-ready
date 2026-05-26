package cn.aiedge.erp.metrics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 指标查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricQueryRequest {
    
    @NotBlank(message = "指标代码不能为空")
    private String metricCode;
    
    private String period;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private String dimensionKey;
    
    private String dimensionValue;
    
    private List<String> metricCodes;
}
