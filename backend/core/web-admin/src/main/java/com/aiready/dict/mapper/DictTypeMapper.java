package com.aiready.dict.mapper;

import com.aiready.dict.entity.DictType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典类型Mapper接口
 */
@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {
    
    /**
     * 检查字典类型编码是否存在
     */
    Integer checkDictTypeCodeExists(@Param("dictTypeCode") String dictTypeCode, @Param("excludeId") Long excludeId);
    
    /**
     * 获取最大排序号
     */
    Integer selectMaxSortOrder();
    
    /**
     * 获取最大排序号（别名，用于兼容性）
     */
    default Integer getMaxSortOrder() {
        return selectMaxSortOrder();
    }
    
    /**
     * 根据字典编码查询
     */
    DictType selectByDictCode(@Param("dictCode") String dictCode);
    
    /**
     * 统计字典项下的字典项数量
     */
    Integer countItemsByDictTypeId(@Param("dictTypeId") Long dictTypeId);
    
    /**
     * 查询所有启用的字典类型
     */
    List<DictType> selectAllActive();
    
    /**
     * 更新状态
     */
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);
    
    /**
     * 检查字典编码是否存在
     */
    Integer existsByDictCode(@Param("dictCode") String dictCode);
    
    /**
     * 检查字典编码是否存在（排除指定ID）
     */
    Integer existsByDictCodeExcludeId(@Param("dictCode") String dictCode, @Param("excludeId") Long excludeId);
}
