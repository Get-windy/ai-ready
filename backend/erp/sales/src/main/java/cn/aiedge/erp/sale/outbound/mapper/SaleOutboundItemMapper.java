package cn.aiedge.erp.sale.outbound.mapper;

import cn.aiedge.erp.sale.outbound.entity.SaleOutboundItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleOutboundItemMapper extends BaseMapper<SaleOutboundItem> {

    @Select("SELECT * FROM erp_sale_outbound_item WHERE outbound_id = #{outboundId} AND deleted = 0 ORDER BY line_no ASC")
    List<SaleOutboundItem> selectByOutboundId(@Param("outboundId") Long outboundId);

    @Select("SELECT SUM(outbound_quantity) FROM erp_sale_outbound_item WHERE outbound_id = #{outboundId} AND deleted = 0")
    java.math.BigDecimal sumOutboundQuantityByOutboundId(@Param("outboundId") Long outboundId);

    @Select("SELECT SUM(line_amount) FROM erp_sale_outbound_item WHERE outbound_id = #{outboundId} AND deleted = 0")
    java.math.BigDecimal sumLineAmountByOutboundId(@Param("outboundId") Long outboundId);
}