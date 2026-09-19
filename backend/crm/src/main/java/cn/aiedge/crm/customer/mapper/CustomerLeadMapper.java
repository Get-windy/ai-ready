package cn.aiedge.crm.customer.mapper;

import cn.aiedge.crm.customer.entity.CustomerLead;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerLeadMapper extends BaseMapper<CustomerLead> {

    /**
     * 当日线索编号的最大值。刻意不带 deleted 条件：唯一索引 uk_crm_lead_code
     * 不含 deleted，已逻辑删除的行仍占号，取号时必须把它们算进来（否则会撞索引）。
     */
    @Select("SELECT MAX(lead_code) FROM crm_customer_lead WHERE lead_code LIKE CONCAT(#{prefix}, '%')")
    String selectMaxLeadCode(@Param("prefix") String prefix);
}