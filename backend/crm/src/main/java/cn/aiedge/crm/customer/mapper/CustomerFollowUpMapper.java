package cn.aiedge.crm.customer.mapper;

import cn.aiedge.crm.customer.entity.CustomerFollowUp;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface CustomerFollowUpMapper extends BaseMapper<CustomerFollowUp> {

    /**
     * 某客户最近一次跟进日期（无跟进记录时返回 {@code null}）。
     *
     * <p>「公海池 · 长期未跟进自动回收」的判定基准。刻意不带 {@code deleted} 条件：
     * 已逻辑删除的跟进记录仍然是"联系过客户"这一事实，算进来才不会把客户误判成长期未跟进。</p>
     */
    @Select("SELECT MAX(follow_up_date) FROM crm_customer_follow_up WHERE customer_id = #{customerId}")
    LocalDate selectMaxFollowUpDate(@Param("customerId") Long customerId);

    /**
     * 当日跟进编号的最大值。刻意不带 deleted 条件：唯一索引 uk_crm_follow_up_code
     * 不含 deleted，已逻辑删除的行仍占号，取号时必须把它们算进来（否则会撞索引）。
     */
    @Select("SELECT MAX(follow_up_code) FROM crm_customer_follow_up WHERE follow_up_code LIKE CONCAT(#{prefix}, '%')")
    String selectMaxFollowUpCode(@Param("prefix") String prefix);
}