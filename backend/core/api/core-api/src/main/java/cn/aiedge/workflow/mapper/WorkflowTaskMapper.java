package cn.aiedge.workflow.mapper;

import cn.aiedge.workflow.entity.WorkflowTaskEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 工作流任务 Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface WorkflowTaskMapper extends BaseMapper<WorkflowTaskEntity> {

    /**
     * 按节点分组的耗时/超时统计（仅审批任务）。
     * 超时判定：节点配置了时限 time_limit（小时）且实际处理时长超过该时限。
     */
    @Select("SELECT t.node_name AS node_name, COUNT(*) AS task_count, "
            + "COALESCE(AVG(EXTRACT(EPOCH FROM (t.handle_time - t.create_time))) "
            + "  FILTER (WHERE t.handle_time IS NOT NULL) / 3600.0, 0) AS avg_hours, "
            + "COUNT(*) FILTER (WHERE n.time_limit IS NOT NULL AND t.handle_time IS NOT NULL "
            + "  AND (t.handle_time - t.create_time) > n.time_limit * INTERVAL '1 hour') AS overdue_count "
            + "FROM workflow_task t LEFT JOIN workflow_node n ON t.node_id = n.id "
            + "WHERE t.deleted = 0 AND t.task_type = 1 "
            + "GROUP BY t.node_name ORDER BY task_count DESC")
    List<Map<String, Object>> selectNodeDurations();

    /**
     * 按日统计任务量/完成量/平均耗时/超时数（仅审批任务），用于效率报表。
     * 日期范围为闭区间 [startDate, endDate]，格式 YYYY-MM-DD。
     */
    @Select("SELECT TO_CHAR(t.create_time, 'YYYY-MM-DD') AS day, "
            + "COUNT(*) AS total_tasks, "
            + "COUNT(*) FILTER (WHERE t.status = 1) AS completed_tasks, "
            + "COALESCE(AVG(EXTRACT(EPOCH FROM (t.handle_time - t.create_time))) "
            + "  FILTER (WHERE t.handle_time IS NOT NULL) / 86400.0, 0) AS avg_days, "
            + "COUNT(*) FILTER (WHERE n.time_limit IS NOT NULL AND t.handle_time IS NOT NULL "
            + "  AND (t.handle_time - t.create_time) > n.time_limit * INTERVAL '1 hour') AS overdue_tasks "
            + "FROM workflow_task t LEFT JOIN workflow_node n ON t.node_id = n.id "
            + "WHERE t.deleted = 0 AND t.task_type = 1 "
            + "AND t.create_time >= #{startDate}::date AND t.create_time < (#{endDate}::date + 1) "
            + "GROUP BY 1 ORDER BY 1")
    List<Map<String, Object>> selectDailyTaskStats(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);
}
