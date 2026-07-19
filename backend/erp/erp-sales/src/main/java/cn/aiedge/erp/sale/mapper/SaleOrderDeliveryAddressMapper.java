package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderDeliveryAddress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleOrderDeliveryAddressMapper extends BaseMapper<SaleOrderDeliveryAddress> {

    @Select("SELECT * FROM erp_sale_order_delivery_address WHERE order_id = #{orderId} ORDER BY sequence")
    List<SaleOrderDeliveryAddress> selectByOrderId(@Param("orderId") Long orderId);
}
