package cn.aiedge.erp.batchsn.mapper;

import cn.aiedge.erp.batchsn.entity.SerialFlowRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 序列号流转记录数据访问接口
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Mapper
public interface SerialFlowRecordMapper extends BaseMapper<SerialFlowRecord> {
    
    /**
     * 查询序列号流转记录
     */
    List<SerialFlowRecord> selectBySerialId(@Param("serialId") Long serialId);
    
    /**
     * 查询序列号完整流转历史
     */
    List<SerialFlowRecord> selectFullHistory(@Param("serialNo") String serialNo);
    
    /**
     * 查询序列号状态变更记录
     */
    List<SerialFlowRecord> selectStatusChangeHistory(
        @Param("serialId") Long serialId,
        @Param("status") String status
    );
    
    /**
     * 查询维修记录
     */
    List<SerialFlowRecord> selectMaintenanceRecords(@Param("serialId") Long serialId);
}
