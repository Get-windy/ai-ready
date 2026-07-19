package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderPartnerSnapshot;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SaleOrderPartnerSnapshotMapper extends BaseMapper<SaleOrderPartnerSnapshot> {

    @Select("SELECT * FROM erp_sale_order_partner_snapshot WHERE order_id = #{orderId}")
    SaleOrderPartnerSnapshot selectByOrderId(@Param("orderId") Long orderId);
}
