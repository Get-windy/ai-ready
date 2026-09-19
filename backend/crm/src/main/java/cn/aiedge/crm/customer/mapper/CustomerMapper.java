package cn.aiedge.crm.customer.mapper;

import cn.aiedge.crm.customer.entity.Customer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 当日客户编号的最大值。刻意不带 deleted 条件：唯一索引 uk_crm_customer_code
     * 不含 deleted，已逻辑删除的行仍占号，取号时必须把它们算进来（否则会撞索引）。
     */
    @Select("SELECT MAX(customer_code) FROM crm_customer WHERE customer_code LIKE CONCAT(#{prefix}, '%')")
    String selectMaxCustomerCode(@Param("prefix") String prefix);
}