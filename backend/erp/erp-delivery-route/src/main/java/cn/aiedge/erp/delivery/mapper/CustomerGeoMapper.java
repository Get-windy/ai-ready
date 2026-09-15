package cn.aiedge.erp.delivery.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 客户配送坐标（biz_party.latitude / longitude）**最小化读写**
 *
 * 说明：坐标是客户主数据的属性，但配送域是唯一消费方（围栏归集 + 地图规划），
 * 故由本模块提供补录入口，只写这两列，不触碰客户主数据的其它字段。
 *
 * ⚠️ 多租户：客户档案含 **tenant_id = 0 的全局数据（散客）**，而单据会引用它；
 * 交由插件统一注入会读不到该行，故此处显式关闭插件并自行按
 * 「当前租户 或 全局(0)」过滤（与 {@code DmsConfigMapper} 同口径，无越权）。
 */
@Mapper
public interface CustomerGeoMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE biz_party SET latitude = #{latitude}, longitude = #{longitude}, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{customerId} AND deleted = 0 AND (tenant_id = #{tenantId} OR tenant_id = 0)")
    int updateGeo(@Param("customerId") Long customerId,
                  @Param("tenantId") Long tenantId,
                  @Param("latitude") BigDecimal latitude,
                  @Param("longitude") BigDecimal longitude);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id AS customer_id, party_name AS customer_name, address, latitude, longitude "
            + "FROM biz_party WHERE deleted = 0 AND (tenant_id = #{tenantId} OR tenant_id = 0) "
            + "ORDER BY id LIMIT #{limit}")
    List<Map<String, Object>> selectCustomerGeos(@Param("tenantId") Long tenantId, @Param("limit") int limit);
}
