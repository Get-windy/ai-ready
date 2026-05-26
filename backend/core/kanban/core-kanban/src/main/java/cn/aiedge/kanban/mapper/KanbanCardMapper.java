package cn.aiedge.kanban.mapper;

import cn.aiedge.kanban.entity.KanbanCard;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KanbanCardMapper extends BaseMapper<KanbanCard> {

    @Select("SELECT * FROM sys_kanban_card WHERE column_id = #{columnId} AND deleted = 0 ORDER BY sort_order ASC")
    List<KanbanCard> selectByColumn(@Param("columnId") Long columnId);

    @Select("SELECT * FROM sys_kanban_card WHERE model_name = #{modelName} AND record_id = #{recordId} AND deleted = 0 LIMIT 1")
    KanbanCard selectByRecord(@Param("modelName") String modelName, @Param("recordId") Long recordId);

    @Select("SELECT COUNT(*) FROM sys_kanban_card WHERE column_id = #{columnId} AND deleted = 0")
    int countByColumn(@Param("columnId") Long columnId);
}