package cn.aiedge.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CostItemCreateDTO {
    
    private String itemCode;
    
    @NotNull(message = "成本项目名称不能为空")
    private String itemName;
    
    @NotNull(message = "成本类型不能为空")
    private Integer costType;
    
    @NotNull(message = "成本中心不能为空")
    private Long costCenterId;
    
    @NotNull(message = "期间不能为空")
    private String period;
    
    private LocalDate costDate;
    
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;
    
    private Long sourceId;
    
    private String sourceType;
    
    private String sourceNo;
    
    private String remark;
}