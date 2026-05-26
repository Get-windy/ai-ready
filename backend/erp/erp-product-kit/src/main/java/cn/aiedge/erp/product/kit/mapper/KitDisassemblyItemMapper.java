package cn.aiedge.erp.product.kit.mapper;

import cn.aiedge.erp.product.kit.entity.KitDisassemblyItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface KitDisassemblyItemMapper extends BaseMapper<KitDisassemblyItem> {

    @Select("SELECT * FROM erp_kit_disassembly_item WHERE disassembly_id = #{disassemblyId} AND deleted = 0 ORDER BY line_no ASC")
    List<KitDisassemblyItem> selectByDisassemblyId(@Param("disassemblyId") Long disassemblyId);

    @Select("SELECT SUM(line_cost) FROM erp_kit_disassembly_item WHERE disassembly_id = #{disassemblyId} AND deleted = 0")
    BigDecimal sumCostByDisassemblyId(@Param("disassemblyId") Long disassemblyId);

    @Select("SELECT SUM(actual_quantity) FROM erp_kit_disassembly_item WHERE disassembly_id = #{disassemblyId} AND deleted = 0")
    BigDecimal sumQuantityByDisassemblyId(@Param("disassemblyId") Long disassemblyId);
}