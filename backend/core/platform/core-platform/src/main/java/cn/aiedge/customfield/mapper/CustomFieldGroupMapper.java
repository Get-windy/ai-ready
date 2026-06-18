package cn.aiedge.customfield.mapper;

import cn.aiedge.customfield.entity.CustomFieldGroup;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CustomFieldGroupMapper extends BaseMapper<CustomFieldGroup> {

    @Select("SELECT * FROM sys_custom_field_group WHERE model_name = #{modelName} AND active = true AND deleted = 0 ORDER BY sort_order ASC")
    List<CustomFieldGroup> selectByModelName(@Param("modelName") String modelName);
}