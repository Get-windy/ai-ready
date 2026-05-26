package cn.aiedge.erp.purchase.inbound.mapper;

import cn.aiedge.erp.purchase.inbound.entity.PurchaseInboundItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PurchaseInboundItemMapper extends BaseMapper<PurchaseInboundItem> {

    @Select("SELECT * FROM erp_purchase_inbound_item WHERE inbound_id = #{inboundId} AND deleted = 0 ORDER BY line_no ASC")
    List<PurchaseInboundItem> selectByInboundId(@Param("inboundId") Long inboundId);

    @Select("SELECT SUM(inbound_quantity) FROM erp_purchase_inbound_item WHERE inbound_id = #{inboundId} AND deleted = 0")
    java.math.BigDecimal sumInboundQuantityByInboundId(@Param("inboundId") Long inboundId);

    @Select("SELECT SUM(line_amount) FROM erp_purchase_inbound_item WHERE inbound_id = #{inboundId} AND deleted = 0")
    java.math.BigDecimal sumLineAmountByInboundId(@Param("inboundId") Long inboundId);

    @Select("SELECT SUM(tax_amount) FROM erp_purchase_inbound_item WHERE inbound_id = #{inboundId} AND deleted = 0")
    java.math.BigDecimal sumTaxAmountByInboundId(@Param("inboundId") Long inboundId);

    @Select("SELECT SUM(line_total) FROM erp_purchase_inbound_item WHERE inbound_id = #{inboundId} AND deleted = 0")
    java.math.BigDecimal sumLineTotalByInboundId(@Param("inboundId") Long inboundId);
}