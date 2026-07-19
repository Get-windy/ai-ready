package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderExtInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SaleOrderExtInfoMapper extends BaseMapper<SaleOrderExtInfo> {

    @Select("SELECT * FROM erp_sale_order_ext_info WHERE order_id = #{orderId}")
    SaleOrderExtInfo selectByOrderId(@Param("orderId") Long orderId);
}
