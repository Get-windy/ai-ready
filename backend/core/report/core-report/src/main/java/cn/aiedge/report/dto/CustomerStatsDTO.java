package cn.aiedge.statistics.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户统计DTO
 */
@Data
public class CustomerStatsDTO {
    private Long tenantId;
    private Integer totalCustomers;
    private Integer newCustomers;
    private Integer activeCustomers;
    private Integer lostCustomers;
    private Integer vipCustomers;
    private List<StageDistribution> stageDistribution;

    @Data
    public static class StageDistribution {
        private Integer stage;
        private String stageName;
        private Integer count;
        private BigDecimal percentage;
    }
}
