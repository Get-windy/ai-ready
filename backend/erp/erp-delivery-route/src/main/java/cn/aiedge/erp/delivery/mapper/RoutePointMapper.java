package cn.aiedge.erp.delivery.mapper;

import cn.aiedge.erp.delivery.entity.RoutePoint;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoutePointMapper extends BaseMapper<RoutePoint> {

    @Select("SELECT * FROM erp_route_point WHERE route_id = #{routeId} ORDER BY point_order ASC")
    List<RoutePoint> selectByRouteId(@Param("routeId") Long routeId);

    @Select("SELECT * FROM erp_route_point WHERE route_id = #{routeId} AND point_order = #{pointOrder}")
    RoutePoint selectByRouteIdAndOrder(@Param("routeId") Long routeId, @Param("pointOrder") Integer pointOrder);

    @Select("SELECT COUNT(*) FROM erp_route_point WHERE route_id = #{routeId} AND status = 'DELIVERED'")
    int countDeliveredByRoute(@Param("routeId") Long routeId);
}