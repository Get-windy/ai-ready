package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.WorkflowNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流节点 Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNode> {

    /**
     * 根据流程定义ID查询节点列表（按顺序排序）
     */
    List<WorkflowNode> selectByDefinitionId(@Param("definitionId") Long definitionId);

    /**
     * 根据流程定义ID和节点编码查询节点
     */
    WorkflowNode selectByCode(@Param("definitionId") Long definitionId, @Param("nodeCode") String nodeCode);

    /**
     * 批量插入节点
     */
    int batchInsert(@Param("list") List<WorkflowNode> list);

    /**
     * 根据流程定义ID删除所有节点
     */
    int deleteByDefinitionId(@Param("definitionId") Long definitionId);
}