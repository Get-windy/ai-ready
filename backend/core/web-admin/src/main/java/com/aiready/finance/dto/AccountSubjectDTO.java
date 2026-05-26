package com.aiready.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会计科目DTO
 */
@Data
public class AccountSubjectDTO {
    
    private Long id;
    
    /**
     * 科目编码
     */
    private String subjectCode;
    
    /**
     * 科目名称
     */
    private String subjectName;
    
    /**
     * 科目类别（1：资产 2：负债 3：权益 4：成本 5：损益）
     */
    private Integer subjectType;
    
    /**
     * 科目类别描述
     */
    private String subjectTypeDesc;
    
    /**
     * 科目级别（1-5级）
     */
    private Integer subjectLevel;
    
    /**
     * 上级科目ID
     */
    private Long parentId;
    
    /**
     * 上级科目名称
     */
    private String parentName;
    
    /**
     * 借贷方向（1：借 2：贷）
     */
    private Integer balanceDirection;
    
    /**
     * 借贷方向描述
     */
    private String balanceDirectionDesc;
    
    /**
     * 期初余额
     */
    private BigDecimal openingBalance;
    
    /**
     * 当前余额
     */
    private BigDecimal currentBalance;
    
    /**
     * 是否明细科目（0：否 1：是）
     */
    private Integer isDetail;
    
    /**
     * 辅助核算类型
     */
    private String auxiliaryTypes;
    
    /**
     * 状态（0：禁用 1：启用）
     */
    private Integer status;
    
    /**
     * 状态描述
     */
    private String statusDesc;
    
    /**
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 子科目列表
     */
    private List<AccountSubjectDTO> children;
}
