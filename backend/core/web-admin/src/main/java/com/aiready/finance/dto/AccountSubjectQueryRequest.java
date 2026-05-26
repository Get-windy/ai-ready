package com.aiready.finance.dto;

import lombok.Data;

/**
 * 会计科目查询请求DTO
 */
@Data
public class AccountSubjectQueryRequest {
    
    /**
     * 科目编码
     */
    private String subjectCode;
    
    /**
     * 科目名称（模糊查询）
     */
    private String subjectName;
    
    /**
     * 科目类别
     */
    private Integer subjectType;
    
    /**
     * 上级科目ID
     */
    private Long parentId;
    
    /**
     * 是否明细科目
     */
    private Integer isDetail;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 当前页码
     */
    private Long current = 1L;
    
    /**
     * 每页大小
     */
    private Long size = 20L;
}
