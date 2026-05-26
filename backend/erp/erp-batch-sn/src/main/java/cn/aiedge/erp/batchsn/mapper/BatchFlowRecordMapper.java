package cn.aiedge.erp.batchsn.mapper;

import cn.aiedge.erp.batchsn.entity.BatchFlowRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 批次流转记录数据访问接口
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Mapper
public interface BatchFlowRecordMapper extends BaseMapper<BatchFlowRecord> {
    
    /**
     * 查询批次流转记录
     */
    List<BatchFlowRecord> selectByBatchId(@Param("batchId") Long batchId);
    
    /**
     * 查询批次流转记录（支持分页）
     */
    List<BatchFlowRecord> selectFlowRecords(
        @Param("batchId") Long batchId,
        @Param("flowType") String flowType,
        @Param("startTime") String startTime,
        @Param("endTime") String endTime
    );
    
    /**
     * 查询产品追溯路径
     */
    List<BatchFlowRecord> selectTraceabilityPath(
        @Param("productId") Long productId,
        @Param("productCode") String productCode
    );
}
