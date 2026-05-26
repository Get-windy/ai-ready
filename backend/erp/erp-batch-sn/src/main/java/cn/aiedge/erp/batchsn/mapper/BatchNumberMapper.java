package cn.aiedge.erp.batchsn.mapper;

import cn.aiedge.erp.batchsn.entity.BatchNumber;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 批次号数据访问接口
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Mapper
public interface BatchNumberMapper extends BaseMapper<BatchNumber> {
    
    /**
     * 根据批次号查询批次
     */
    BatchNumber selectByBatchNo(@Param("batchNo") String batchNo);
    
    /**
     * 查询即将过期的批次（临期预警）
     */
    List<BatchNumber> selectExpiringBatches(@Param("warningDays") int warningDays, @Param("currentTime") Date currentTime);
    
    /**
     * 查询过期批次
     */
    List<BatchNumber> selectExpiredBatches(@Param("currentTime") Date currentTime);
    
    /**
     * 批量更新批次状态
     */
    int updateBatchStatus(@Param("batchIds") List<Long> batchIds, @Param("newStatus") String newStatus, @Param("updatedBy") String updatedBy);
    
    /**
     * 更新可用数量
     */
    int updateAvailableQuantity(@Param("batchId") Long batchId, @Param("quantityChange") BigDecimal quantityChange);
    
    /**
     * 查询批次库存汇总
     */
    List<BatchNumber> selectStockSummary();
    
    /**
     * 查询产品批次历史
     */
    List<BatchNumber> selectProductBatchHistory(@Param("productId") Long productId, @Param("productCode") String productCode);
    
    /**
     * 查询指定时间范围内的批次
     */
    List<BatchNumber> selectBatchesByDateRange(
        @Param("startTime") Date startTime,
        @Param("endTime") Date endTime,
        @Param("status") String status
    );
}
