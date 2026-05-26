package cn.aiedge.erp.batchsn.mapper;

import cn.aiedge.erp.batchsn.entity.SerialNumber;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

/**
 * 序列号数据访问接口
 * 
 * @author team-member
 * @date 2026-04-27
 */
@Mapper
public interface SerialNumberMapper extends BaseMapper<SerialNumber> {
    
    /**
     * 根据序列号查询
     */
    SerialNumber selectBySerialNo(@Param("serialNo") String serialNo);
    
    /**
     * 查询质保即将到期的序列号
     */
    List<SerialNumber> selectWarrantyExpiring(
        @Param("warningDays") int warningDays,
        @Param("currentTime") Date currentTime
    );
    
    /**
     * 查询质保已到期的序列号
     */
    List<SerialNumber> selectWarrantyExpired(@Param("currentTime") Date currentTime);
    
    /**
     * 查询序列号流转历史
     */
    List<SerialNumber> selectFlowHistory(@Param("serialNo") String serialNo);
    
    /**
     * 根据产品查询序列号列表
     */
    List<SerialNumber> selectByProductId(@Param("productId") Long productId);
    
    /**
     * 查询指定状态的序列号
     */
    List<SerialNumber> selectByStatus(@Param("status") String status);
    
    /**
     * 批量更新序列号状态
     */
    int updateSerialStatus(
        @Param("serialIds") List<Long> serialIds,
        @Param("newStatus") String newStatus,
        @Param("newStage") String newStage,
        @Param("updaterId") String updaterId
    );
}
