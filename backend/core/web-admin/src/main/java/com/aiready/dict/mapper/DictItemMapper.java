package com.aiready.dict.mapper;

import com.aiready.dict.entity.DictItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典项Mapper接口
 */
@Mapper
public interface DictItemMapper extends BaseMapper<DictItem> {
    
    /**
     * 根据字典类型ID获取字典项列表
     */
    List<DictItem> selectByDictTypeId(@Param("dictTypeId") Long dictTypeId);
    
    /**
     * 检查字典项编码在指定类型下是否存在
     */
    Integer checkDictItemCodeExists(@Param("dictTypeId") Long dictTypeId, 
                                  @Param("dictItemCode") String dictItemCode, 
                                  @Param("excludeId") Long excludeId);
    
    /**
     * 根据字典类型编码获取字典项列表
     */
    List<DictItem> selectByDictTypeCode(@Param("dictTypeCode") String dictTypeCode);
    
    /**
     * 根据字典项值获取字典项
     */
    DictItem selectByTypeCodeAndValue(@Param("dictTypeCode") String dictTypeCode, 
                                    @Param("dictItemValue") String dictItemValue);
    
    /**
     * 根据字典项编码获取字典项
     */
    DictItem selectByTypeCodeAndCode(@Param("dictTypeCode") String dictTypeCode, 
                                   @Param("dictItemCode") String dictItemCode);
    
    /**
     * 批量插入字典项
     */
    int batchInsert(@Param("dictItems") List<DictItem> dictItems);
    
    /**
     * 根据字典类型ID删除字典项
     */
    int deleteByDictTypeId(@Param("dictTypeId") Long dictTypeId);
}
