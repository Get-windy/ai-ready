package com.aiready.dict.dto;

import lombok.Data;

import java.util.List;

/**
 * 字典项批量操作请求
 */
@Data
public class DictItemBatchRequest {
    
    /**
     * 字典类型ID
     */
    private Long dictTypeId;
    
    /**
     * 字典项列表
     */
    private List<DictItemSaveRequest> items;
    
    /**
     * 要删除的字典项ID列表
     */
    private List<Long> deleteIds;
}
