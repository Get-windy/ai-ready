package cn.aiedge.customfield.mapper;

import cn.aiedge.customfield.entity.CustomField;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CustomFieldMapper extends BaseMapper<CustomField> {

    @Select("SELECT * FROM sys_custom_field WHERE model_name = #{modelName} AND active = true AND deleted = 0 ORDER BY sort_order ASC")
    List<CustomField> selectByModelName(@Param("modelName") String modelName);

    @Select("SELECT * FROM sys_custom_field WHERE model_name = #{modelName} AND group_code = #{groupCode} AND active = true AND deleted = 0 ORDER BY sort_order ASC")
    List<CustomField> selectByModelAndGroup(@Param("modelName") String modelName, @Param("groupCode") String groupCode);

    @Select("SELECT * FROM sys_custom_field WHERE model_name = #{modelName} AND searchable = true AND active = true AND deleted = 0")
    List<CustomField> selectSearchableFields(@Param("modelName") String modelName);

    @Select("SELECT COUNT(*) FROM sys_custom_field WHERE model_name = #{modelName} AND field_name = #{fieldName} AND deleted = 0")
    int countByModelAndFieldName(@Param("modelName") String modelName, @Param("fieldName") String fieldName);
}