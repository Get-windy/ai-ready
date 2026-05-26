package cn.aiedge.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AccountPeriodCreateDTO {
    
    @NotNull(message = "年份不能为空")
    private Integer year;
    
    @NotNull(message = "月份不能为空")
    private Integer month;
    
    private String remark;
}