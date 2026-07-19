package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderLogistics;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleOrderLogisticsMapper extends BaseMapper<SaleOrderLogistics> {

    @Select("SELECT * FROM erp_sale_order_logistics WHERE order_id = #{orderId} ORDER BY create_time")
    List<SaleOrderLogistics> selectByOrderId(@Param("orderId") Long orderId);
}
