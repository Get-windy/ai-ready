package cn.aiedge.customfield.mapper;

import cn.aiedge.customfield.entity.CustomFieldValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CustomFieldValueMapper extends BaseMapper<CustomFieldValue> {

    @Select("SELECT * FROM sys_custom_field_value WHERE model_name = #{modelName} AND record_id = #{recordId}")
    List<CustomFieldValue> selectByRecord(@Param("modelName") String modelName, @Param("recordId") Long recordId);

    @Select("SELECT * FROM sys_custom_field_value WHERE field_id = #{fieldId} AND record_id = #{recordId} LIMIT 1")
    CustomFieldValue selectByFieldAndRecord(@Param("fieldId") Long fieldId, @Param("recordId") Long recordId);

    @Select("SELECT DISTINCT record_id FROM sys_custom_field_value WHERE field_id = #{fieldId} AND value_string LIKE #{value}")
    List<Long> searchByStringValue(@Param("fieldId") Long fieldId, @Param("value") String value);

    @Select("SELECT DISTINCT record_id FROM sys_custom_field_value WHERE field_id = #{fieldId} AND value_integer = #{value}")
    List<Long> searchByIntegerValue(@Param("fieldId") Long fieldId, @Param("value") Integer value);

    @Select("SELECT DISTINCT record_id FROM sys_custom_field_value WHERE field_id = #{fieldId} AND value_boolean = #{value}")
    List<Long> searchByBooleanValue(@Param("fieldId") Long fieldId, @Param("value") Boolean value);
}