package cn.aiedge.order.mapper;

import cn.aiedge.order.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Update("UPDATE erp_order SET status = #{status}, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE erp_order SET received_amount = received_amount + #{amount}, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int addReceivedAmount(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);

    @Update("UPDATE erp_order SET follow_count = follow_count + 1, last_follow_time = #{lastFollowTime}, update_time = NOW() WHERE id = #{id} AND deleted = 0")
    int incrementFollowCount(@Param("id") Long id, @Param("lastFollowTime") LocalDateTime lastFollowTime);
}
