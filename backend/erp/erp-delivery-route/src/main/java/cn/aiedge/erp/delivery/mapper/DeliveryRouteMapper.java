package cn.aiedge.erp.delivery.mapper;

import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DeliveryRouteMapper extends BaseMapper<DeliveryRoute> {

    @Select("SELECT * FROM erp_delivery_route WHERE delivery_person_id = #{deliveryPersonId} AND status = 'IN_PROGRESS' ORDER BY create_time DESC LIMIT 1")
    DeliveryRoute selectActiveRouteByPerson(@Param("deliveryPersonId") String deliveryPersonId);

    @Select("SELECT COUNT(*) FROM erp_delivery_route WHERE delivery_person_id = #{deliveryPersonId} AND status IN ('READY', 'IN_PROGRESS')")
    int countActiveRoutesByPerson(@Param("deliveryPersonId") String deliveryPersonId);
}