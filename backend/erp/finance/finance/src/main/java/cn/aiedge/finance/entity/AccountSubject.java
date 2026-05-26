package cn.aiedge.finance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("finance_account_subject")
public class AccountSubject {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private String subjectCode;
    
    private String subjectName;
    
    private Integer subjectType;
    
    private Integer level;
    
    private Long parentId;
    
    private Integer balanceDirection;
    
    private Integer auxiliaryFlag;
    
    private String auxiliaryTypes;
    
    private Integer enabled;
    
    private Integer leafFlag;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
    
    private Long tenantId;
    
    @Version
    private Integer version;
    
    @TableField(exist = false)
    private List<AccountSubject> children;
}