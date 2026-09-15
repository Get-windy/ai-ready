package cn.aiedge.dms.dispatch.mapper;

import cn.aiedge.dms.dispatch.dto.RouteOptionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 线路档案只读查询（区域分包绑定的「线路」选择器 / 名称快照回填）
 *
 * <p>红线：线路是《资料 → 配送管理 → 线路》的**同一主数据**（`erp_route`），DMS 侧只读引用，
 * 不新建线路表、不写线路数据；取数口径与《线路》一致（未删除 + 已启用）。</p>
 *
 * @author AI-Ready Team
 */
@Mapper
public interface RouteLookupMapper {

    /** 线路候选：按编号/名称模糊，最多 limit 条 */
    @Select("<script>"
            + "SELECT r.id AS id, r.route_code AS routeCode, r.route_name AS routeName, "
            + "       r.route_self AS routeSelf, r.route_logistics AS routeLogistics, r.express_name AS expressName "
            + "FROM erp_route r "
            + "WHERE r.deleted = 0 AND r.status = 'ENABLED' "
            + "<if test=\"keyword != null\">"
            + "  AND (r.route_name LIKE CONCAT('%', #{keyword}, '%') "
            + "       OR r.route_code LIKE CONCAT('%', #{keyword}, '%')) "
            + "</if>"
            + "ORDER BY r.route_name ASC LIMIT #{limit}"
            + "</script>")
    List<RouteOptionVO> searchRoutes(@Param("keyword") String keyword, @Param("limit") int limit);

    /** 按ID取线路（保存绑定时校验并回填编号/名称快照） */
    @Select("SELECT r.id AS id, r.route_code AS routeCode, r.route_name AS routeName, "
            + "       r.route_self AS routeSelf, r.route_logistics AS routeLogistics, r.express_name AS expressName "
            + "FROM erp_route r WHERE r.id = #{id} AND r.deleted = 0")
    RouteOptionVO findRoute(@Param("id") Long id);
}
