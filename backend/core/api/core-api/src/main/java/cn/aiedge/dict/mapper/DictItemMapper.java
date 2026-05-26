package cn.aiedge.dict.mapper;

import cn.aiedge.dict.model.DictItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典项Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface DictItemMapper extends BaseMapper<DictItem> {

    /**
     * 查询字典项树形结构
     *
     * @param dictTypeId 字典类型ID
     * @param parentId   父ID
     * @return 字典项列表
     */
    List<DictItem> selectTree(@Param("dictTypeId") Long dictTypeId, @Param("parentId") Long parentId);

    /**
     * 根据字典类型查询启用的字典项
     *
     * @param dictTypeId 字典类型ID
     * @return 字典项列表
     */
    List<DictItem> selectEnabledByType(@Param("dictTypeId") Long dictTypeId);

    /**
     * 根据字典类型编码和项值查询字典项
     *
     * @param dictCode  字典类型编码
     * @param itemValue 字典项值
     * @param tenantId  租户ID
     * @return 字典项
     */
    DictItem selectByDictCodeAndValue(@Param("dictCode") String dictCode, 
                                       @Param("itemValue") String itemValue, 
                                       @Param("tenantId") Long tenantId);

    /**
     * 批量查询字典项
     *
     * @param dictTypeIds 字典类型ID列表
     * @return 字典项列表
     */
    List<DictItem> selectByDictTypeIds(@Param("dictTypeIds") List<Long> dictTypeIds);

    /**
     * 查询字典项数量
     *
     * @param dictTypeId 字典类型ID
     * @return 数量
     */
    int countByDictType(@Param("dictTypeId") Long dictTypeId);
}
