package cn.aiedge.workflow.mapper;

import cn.aiedge.workflow.entity.WorkflowInstanceEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 工作流实例 Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface WorkflowInstanceMapper extends BaseMapper<WorkflowInstanceEntity> {

    /**
     * 实例总体统计：总数 / 运行中(状态0) / 已完成实例平均耗时（天）
     */
    @Select("SELECT COUNT(*) AS total_instances, "
            + "COUNT(*) FILTER (WHERE status = 0) AS running_instances, "
            + "COALESCE(AVG(EXTRACT(EPOCH FROM (finish_time - create_time))) "
            + "  FILTER (WHERE finish_time IS NOT NULL) / 86400.0, 0) AS avg_duration_days "
            + "FROM workflow_instance WHERE deleted = 0")
    Map<String, Object> selectOverallStats();

    /**
     * 按流程分组的耗时统计（仅统计已完成实例的耗时）
     */
    @Select("SELECT d.process_name AS process_name, COUNT(i.id) AS instance_count, "
            + "COALESCE(AVG(EXTRACT(EPOCH FROM (i.finish_time - i.create_time))) "
            + "  FILTER (WHERE i.finish_time IS NOT NULL) / 86400.0, 0) AS avg_days, "
            + "COALESCE(MAX(EXTRACT(EPOCH FROM (i.finish_time - i.create_time))) "
            + "  / 86400.0, 0) AS max_days, "
            + "COALESCE(MIN(EXTRACT(EPOCH FROM (i.finish_time - i.create_time))) "
            + "  FILTER (WHERE i.finish_time IS NOT NULL) / 86400.0, 0) AS min_days "
            + "FROM workflow_instance i JOIN workflow_definition d ON i.definition_id = d.id "
            + "WHERE i.deleted = 0 AND d.deleted = 0 "
            + "GROUP BY d.process_name ORDER BY instance_count DESC")
    List<Map<String, Object>> selectProcessDurations();
}
