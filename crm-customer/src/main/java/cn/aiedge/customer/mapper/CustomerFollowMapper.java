package cn.aiedge.customer.mapper;

import cn.aiedge.customer.entity.CustomerFollow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 客户跟进记录Mapper
 */
@Mapper
public interface CustomerFollowMapper extends BaseMapper<CustomerFollow> {

    @Select("SELECT * FROM crm_customer_follow WHERE customer_id = #{customerId} ORDER BY follow_time DESC")
    List<CustomerFollow> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("SELECT * FROM crm_customer_follow WHERE follower_id = #{followerId} ORDER BY follow_time DESC LIMIT #{limit}")
    List<CustomerFollow> selectRecentByFollower(@Param("followerId") Long followerId, @Param("limit") int limit);
}