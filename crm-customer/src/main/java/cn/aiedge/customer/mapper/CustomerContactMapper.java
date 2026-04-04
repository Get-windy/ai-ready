package cn.aiedge.customer.mapper;

import cn.aiedge.customer.entity.CustomerContact;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 客户联系人Mapper
 */
@Mapper
public interface CustomerContactMapper extends BaseMapper<CustomerContact> {

    @Select("SELECT * FROM crm_customer_contact WHERE customer_id = #{customerId} AND status = 0 ORDER BY is_key_decision_maker DESC, id")
    List<CustomerContact> selectByCustomerId(@Param("customerId") Long customerId);
}