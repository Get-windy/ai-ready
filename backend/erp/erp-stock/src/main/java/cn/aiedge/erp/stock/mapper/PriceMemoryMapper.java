package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.PriceMemory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PriceMemoryMapper extends BaseMapper<PriceMemory> {

    @Select("SELECT * FROM erp_price_memory " +
            "WHERE product_id = #{productId} AND partner_id = #{partnerId} AND biz_type = #{bizType} " +
            "AND is_latest = 1 AND deleted = 0 LIMIT 1")
    PriceMemory selectLatest(@Param("productId") Long productId,
                              @Param("partnerId") Long partnerId,
                              @Param("bizType") String bizType);
}
