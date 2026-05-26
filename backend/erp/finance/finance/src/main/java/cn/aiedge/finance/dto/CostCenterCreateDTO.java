package cn.aiedge.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CostCenterCreateDTO {
    
    @NotBlank(message = "成本中心编码不能为空")
    private String centerCode;
    
    @NotBlank(message = "成本中心名称不能为空")
    private String centerName;
    
    @NotNull(message = "成本中心类型不能为空")
    private Integer centerType;
    
    private Long parentId;
    
    private Long departmentId;
    
    private String departmentName;
    
    private Long projectId;
    
    private String projectName;
    
    private Integer allocationMethod;
    
    private BigDecimal allocationRate;
    
    private String allocationBase;
    
    private String remark;
}