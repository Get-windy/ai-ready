package cn.aiedge.erp.sale.mapper;

import cn.aiedge.erp.sale.entity.SaleOrderAuditTrail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SaleOrderAuditTrailMapper extends BaseMapper<SaleOrderAuditTrail> {

    @Select("SELECT * FROM erp_sale_order_audit_trail WHERE order_id = #{orderId} ORDER BY action_time")
    List<SaleOrderAuditTrail> selectByOrderId(@Param("orderId") Long orderId);
}
