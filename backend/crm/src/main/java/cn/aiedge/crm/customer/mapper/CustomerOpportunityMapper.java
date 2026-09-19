package cn.aiedge.crm.customer.mapper;

import cn.aiedge.crm.customer.entity.CustomerOpportunity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerOpportunityMapper extends BaseMapper<CustomerOpportunity> {

    /**
     * 当日商机编号的最大值。刻意不带 deleted 条件：唯一索引 uk_crm_opportunity_code
     * 不含 deleted，已逻辑删除的行仍占号，取号时必须把它们算进来（否则会撞索引）。
     */
    @Select("SELECT MAX(opportunity_code) FROM crm_customer_opportunity WHERE opportunity_code LIKE CONCAT(#{prefix}, '%')")
    String selectMaxOpportunityCode(@Param("prefix") String prefix);
}