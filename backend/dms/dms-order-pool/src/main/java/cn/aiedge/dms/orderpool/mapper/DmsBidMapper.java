package cn.aiedge.dms.orderpool.mapper;

import cn.aiedge.dms.orderpool.entity.DmsBid;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 竞价记录 Mapper
 */
@Mapper
public interface DmsBidMapper extends BaseMapper<DmsBid> {

    /**
     * 根据订单大厅 ID 查询竞价记录，按出价升序排列
     */
    @Select("SELECT * FROM dms_bid WHERE pool_id = #{poolId} ORDER BY bid_price ASC")
    List<DmsBid> findByPoolId(Long poolId);
}
