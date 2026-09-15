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
     *
     * <p>必须显式带 {@code deleted = 0}：MyBatis-Plus 的逻辑删除只作用于其自动生成的 SQL，
     * 手写 {@code @Select} 不会被追加逻辑删除条件。漏掉会把「已取消的出价」当成有效报价返回，
     * 既污染竞价列表，也会让 {@code settleBid} 选中一位已退出的配送员。
     * tenant_id 由 TenantLineInnerInterceptor 自动注入。</p>
     */
    @Select("SELECT * FROM dms_bid WHERE pool_id = #{poolId} AND deleted = 0 ORDER BY bid_price ASC, id ASC")
    List<DmsBid> findByPoolId(Long poolId);

    /**
     * 查询某配送员在某池中的有效（未取消）出价，用于「同一人重复出价只保留最新一条」
     */
    @Select("SELECT * FROM dms_bid WHERE pool_id = #{poolId} AND rider_id = #{riderId} AND deleted = 0 "
            + "ORDER BY id DESC LIMIT 1")
    DmsBid findActiveBid(Long poolId, Long riderId);

    /**
     * 查询某池当前最低有效出价（竞价中展示「当前价」；出价被取消后需回退到剩余最低价）
     */
    @Select("SELECT * FROM dms_bid WHERE pool_id = #{poolId} AND deleted = 0 ORDER BY bid_price ASC, id ASC LIMIT 1")
    DmsBid findLowestBid(Long poolId);
}
