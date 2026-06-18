package cn.aiedge.permission.mapper;

import cn.aiedge.permission.entity.FieldPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FieldPermissionMapper extends BaseMapper<FieldPermission> {

    @Select("SELECT * FROM sys_field_permission WHERE model_name = #{modelName} AND field_name = #{fieldName} AND deleted = 0")
    List<FieldPermission> selectByModelAndField(@Param("modelName") String modelName, @Param("fieldName") String fieldName);

    @Select("SELECT * FROM sys_field_permission WHERE model_name = #{modelName} AND user_id = #{userId} AND deleted = 0")
    List<FieldPermission> selectByModelAndUser(@Param("modelName") String modelName, @Param("userId") Long userId);

    @Select("SELECT * FROM sys_field_permission WHERE model_name = #{modelName} AND group_id = #{groupId} AND deleted = 0")
    List<FieldPermission> selectByModelAndGroup(@Param("modelName") String modelName, @Param("groupId") Long groupId);

    @Select("SELECT * FROM sys_field_permission WHERE model_name = #{modelName} AND deleted = 0")
    List<FieldPermission> selectByModel(@Param("modelName") String modelName);

    @Select("SELECT * FROM sys_field_permission WHERE model_name = #{modelName} AND field_name = #{fieldName} AND user_id = #{userId} AND deleted = 0 LIMIT 1")
    FieldPermission selectByModelFieldUser(@Param("modelName") String modelName, @Param("fieldName") String fieldName, @Param("userId") Long userId);

    @Select("SELECT * FROM sys_field_permission WHERE model_name = #{modelName} AND field_name = #{fieldName} AND group_id = #{groupId} AND deleted = 0 LIMIT 1")
    FieldPermission selectByModelFieldGroup(@Param("modelName") String modelName, @Param("fieldName") String fieldName, @Param("groupId") Long groupId);
}