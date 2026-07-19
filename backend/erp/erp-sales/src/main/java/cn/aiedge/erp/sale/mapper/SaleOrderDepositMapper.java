package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderDeposit;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleOrderDepositMapper extends BaseMapper<SaleOrderDeposit> {

    @Select("SELECT * FROM erp_sale_order_deposit WHERE order_id = #{orderId} ORDER BY sequence")
    List<SaleOrderDeposit> selectByOrderId(@Param("orderId") Long orderId);
}
