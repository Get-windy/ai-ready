package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 销售订单明细Mapper
 */
@Mapper
public interface SaleOrderItemMapper extends BaseMapper<SaleOrderItem> {

    /**
     * 根据订单ID查询明细
     */
    @Select("SELECT * FROM erp_sale_order_item WHERE order_id = #{orderId} ORDER BY line_no")
    List<SaleOrderItem> selectByOrderId(@Param("orderId") Long orderId);

    /**
     * 更新已出库数量
     */
    @Select("UPDATE erp_sale_order_item SET shipped_quantity = shipped_quantity + #{quantity} WHERE id = #{id}")
    int addShippedQuantity(@Param("id") Long id, @Param("quantity") java.math.BigDecimal quantity);
}