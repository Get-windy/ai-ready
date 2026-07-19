package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderSettlement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SaleOrderSettlementMapper extends BaseMapper<SaleOrderSettlement> {

    @Select("SELECT * FROM erp_sale_order_settlement WHERE order_id = #{orderId}")
    SaleOrderSettlement selectByOrderId(@Param("orderId") Long orderId);
}
