package cn.aiedge.kanban.mapper;

import cn.aiedge.kanban.entity.KanbanColumn;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KanbanColumnMapper extends BaseMapper<KanbanColumn> {

    @Select("SELECT * FROM sys_kanban_column WHERE model_name = #{modelName} AND group_field = #{groupField} AND active = true AND deleted = 0 ORDER BY sort_order ASC")
    List<KanbanColumn> selectByModelAndGroupField(@Param("modelName") String modelName, @Param("groupField") String groupField);

    @Select("SELECT * FROM sys_kanban_column WHERE model_name = #{modelName} AND group_field = #{groupField} AND group_value = #{groupValue} AND deleted = 0 LIMIT 1")
    KanbanColumn selectByGroupValue(@Param("modelName") String modelName, @Param("groupField") String groupField, @Param("groupValue") String groupValue);
}