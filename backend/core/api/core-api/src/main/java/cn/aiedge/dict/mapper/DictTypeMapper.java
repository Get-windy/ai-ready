package cn.aiedge.dict.mapper;

import cn.aiedge.dict.model.DictType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典类型Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface DictTypeMapper extends BaseMapper<DictType> {

    /**
     * 查询字典类型树形结构
     *
     * @param tenantId 租户ID
     * @param parentId 父ID
     * @return 字典类型列表
     */
    List<DictType> selectTree(@Param("tenantId") Long tenantId, @Param("parentId") Long parentId);

    /**
     * 根据编码查询字典类型
     *
     * @param dictCode 字典编码
     * @param tenantId 租户ID
     * @return 字典类型
     */
    DictType selectByDictCode(@Param("dictCode") String dictCode, @Param("tenantId") Long tenantId);

    /**
     * 查询所有启用的字典类型
     *
     * @param tenantId 租户ID
     * @return 字典类型列表
     */
    List<DictType> selectEnabled(@Param("tenantId") Long tenantId);
}
