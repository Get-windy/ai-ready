package cn.aiedge.finance.dto;

import lombok.Data;

import java.util.List;

@Data
public class AccountSubjectVO {
    
    private Long id;
    
    private String subjectCode;
    
    private String subjectName;
    
    private Integer subjectType;
    
    private String subjectTypeName;
    
    private Integer level;
    
    private Long parentId;
    
    private String parentName;
    
    private Integer balanceDirection;
    
    private String balanceDirectionName;
    
    private Integer auxiliaryFlag;
    
    private String auxiliaryTypes;
    
    private Integer enabled;
    
    private Integer leafFlag;
    
    private String remark;
    
    private List<AccountSubjectVO> children;
}