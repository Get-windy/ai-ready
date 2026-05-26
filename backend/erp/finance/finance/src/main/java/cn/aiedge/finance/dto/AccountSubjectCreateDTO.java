package cn.aiedge.finance.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class AccountSubjectCreateDTO {
    
    @NotBlank(message = "科目编码不能为空")
    private String subjectCode;
    
    @NotBlank(message = "科目名称不能为空")
    private String subjectName;
    
    @NotNull(message = "科目类型不能为空")
    private Integer subjectType;
    
    @NotNull(message = "科目级别不能为空")
    private Integer level;
    
    private Long parentId;
    
    @NotNull(message = "余额方向不能为空")
    private Integer balanceDirection;
    
    private Integer auxiliaryFlag;
    
    private String auxiliaryTypes;
    
    private String remark;
}