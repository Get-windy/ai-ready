package cn.aiedge.erp.delivery.route.mapper;

import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 行政区划只读查询（线路「配送区域」引用 sys_region，无 tenant_id，不参与多租户隔离）
 */
@Mapper
public interface RegionLookupMapper {

    @Select("SELECT code, name, region_level FROM sys_region WHERE code = #{code} AND status = 1 LIMIT 1")
    RegionRow selectByCode(@Param("code") String code);

    @Data
    class RegionRow {
        private String code;
        private String name;
        private Integer regionLevel;
    }
}
