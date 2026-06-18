package cn.aiedge.erp.product.kit.mapper;

import cn.aiedge.erp.product.kit.entity.KitAssemblyItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface KitAssemblyItemMapper extends BaseMapper<KitAssemblyItem> {

    @Select("SELECT * FROM erp_kit_assembly_item WHERE assembly_id = #{assemblyId} AND deleted = 0 ORDER BY line_no ASC")
    List<KitAssemblyItem> selectByAssemblyId(@Param("assemblyId") Long assemblyId);

    @Select("SELECT SUM(line_cost) FROM erp_kit_assembly_item WHERE assembly_id = #{assemblyId} AND deleted = 0")
    BigDecimal sumCostByAssemblyId(@Param("assemblyId") Long assemblyId);

    @Select("SELECT SUM(actual_quantity) FROM erp_kit_assembly_item WHERE assembly_id = #{assemblyId} AND deleted = 0")
    BigDecimal sumQuantityByAssemblyId(@Param("assemblyId") Long assemblyId);
}