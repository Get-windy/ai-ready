package cn.aiedge.customer.mapper;

import cn.aiedge.customer.entity.Customer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 客户Mapper
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @Select("SELECT * FROM crm_customer WHERE customer_code = #{customerCode} AND tenant_id = #{tenantId}")
    Customer selectByCode(@Param("customerCode") String customerCode, @Param("tenantId") Long tenantId);

    @Update("UPDATE crm_customer SET follow_count = follow_count + 1, last_follow_time = #{lastFollowTime}, update_time = NOW() WHERE id = #{id}")
    int incrementFollowCount(@Param("id") Long id, @Param("lastFollowTime") java.time.LocalDateTime lastFollowTime);

    @Update("UPDATE crm_customer SET stage = #{stage}, update_time = NOW() WHERE id = #{id}")
    int updateStage(@Param("id") Long id, @Param("stage") Integer stage);

    @Select("SELECT * FROM crm_customer WHERE owner_id = #{ownerId} AND stage IN (1, 2) ORDER BY next_follow_time ASC")
    List<Customer> selectPendingFollow(@Param("ownerId") Long ownerId);

    @Select("SELECT * FROM crm_customer WHERE tenant_id = #{tenantId} AND stage = #{stage}")
    List<Customer> selectByStage(@Param("tenantId") Long tenantId, @Param("stage") Integer stage);
}