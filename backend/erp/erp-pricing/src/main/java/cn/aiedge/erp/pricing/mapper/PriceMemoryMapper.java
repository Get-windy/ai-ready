package cn.aiedge.erp.pricing.mapper;

import cn.aiedge.erp.pricing.entity.PriceMemory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PriceMemoryMapper extends BaseMapper<PriceMemory> {

    @Select("SELECT * FROM erp_price_memory " +
            "WHERE product_id = #{productId} AND customer_id = #{customerId} AND biz_type = #{bizType} " +
            "AND is_latest = 1 AND deleted = 0 LIMIT 1")
    PriceMemory selectLatest(@Param("productId") Long productId,
                             @Param("customerId") Long customerId,
                             @Param("bizType") String bizType);
}