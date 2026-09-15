package cn.aiedge.erp.delivery.mapper;

import cn.aiedge.erp.delivery.entity.RoutePoint;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoutePointMapper extends BaseMapper<RoutePoint> {

    @Select("SELECT * FROM erp_route_point WHERE route_id = #{routeId} ORDER BY point_order ASC, id ASC")
    List<RoutePoint> selectByRouteId(@Param("routeId") Long routeId);

    @Select("SELECT COUNT(*) FROM erp_route_point WHERE route_id = #{routeId} AND status = 'DELIVERED'")
    int countDeliveredByRoute(@Param("routeId") Long routeId);

    @Select("SELECT COUNT(*) FROM erp_route_point WHERE route_id = #{routeId} AND status = 'FAILED'")
    int countFailedByRoute(@Param("routeId") Long routeId);

    @Select("SELECT COUNT(*) FROM erp_route_point WHERE route_id = #{routeId}")
    int countByRoute(@Param("routeId") Long routeId);
}
