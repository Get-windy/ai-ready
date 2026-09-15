package cn.aiedge.erp.delivery.mapper;

import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import cn.aiedge.erp.delivery.route.entity.RouteMaster;
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

    /**
     * 引用线路档案（资料 → 配送管理 → 线路，erp_route）回填线路名称/类型快照。
     * ⚠️ 只读引用，不在本表落线路档案字段 —— 执行单与档案严格分表（红线）。
     */
    @Select("SELECT id, route_code, route_name, route_self, route_logistics, express_name, status, remark "
            + "FROM erp_route WHERE id = #{id} AND deleted = 0")
    RouteMaster selectRouteMasterById(@Param("id") Long id);

    /** 按前缀查询当日已有编号（号段生成用） */
    @Select("SELECT route_code FROM erp_delivery_route WHERE route_code LIKE #{prefix} AND deleted = 0")
    List<String> selectCodesByPrefix(@Param("prefix") String prefix);
}