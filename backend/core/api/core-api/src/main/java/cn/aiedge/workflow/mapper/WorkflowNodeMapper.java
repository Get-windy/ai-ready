package cn.aiedge.workflow.mapper;

import cn.aiedge.workflow.entity.WorkflowNodeEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工作流节点 Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNodeEntity> {

    /**
     * 按行ID查询节点（含已逻辑删除）。
     * 定义更新后旧节点被逻辑删除，但历史实例的 current_node_id 仍指向旧行，解析时需绕过逻辑删除过滤。
     */
    @Select("SELECT * FROM workflow_node WHERE id = #{id}")
    WorkflowNodeEntity selectByIdIncludeDeleted(@Param("id") Long id);
}
