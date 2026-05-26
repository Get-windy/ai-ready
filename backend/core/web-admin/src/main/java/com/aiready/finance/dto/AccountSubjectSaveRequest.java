package com.aiready.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会计科目保存请求DTO
 */
@Data
public class AccountSubjectSaveRequest {
    
    /**
     * 科目编码
     */
    @NotBlank(message = "科目编码不能为空")
    private String subjectCode;
    
    /**
     * 科目名称
     */
    @NotBlank(message = "科目名称不能为空")
    private String subjectName;
    
    /**
     * 科目类别（1：资产 2：负债 3：权益 4：成本 5：损益）
     */
    @NotNull(message = "科目类别不能为空")
    private Integer subjectType;
    
    /**
     * 科目级别（1-5级）
     */
    @NotNull(message = "科目级别不能为空")
    private Integer subjectLevel;
    
    /**
     * 上级科目ID
     */
    private Long parentId;
    
    /**
     * 借贷方向（1：借 2：贷）
     */
    @NotNull(message = "借贷方向不能为空")
    private Integer balanceDirection;
    
    /**
     * 期初余额
     */
    private BigDecimal openingBalance;
    
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
     * 排序顺序
     */
    private Integer sortOrder;
    
    /**
     * 备注
     */
    private String remark;
}
