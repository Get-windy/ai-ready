package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.StockCheckItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StockCheckItemMapper extends BaseMapper<StockCheckItem> {

    @Select("SELECT * FROM erp_stock_check_item WHERE check_id = #{checkId} AND deleted = 0 ORDER BY line_no ASC")
    List<StockCheckItem> selectByCheckId(@Param("checkId") Long checkId);

    @Select("SELECT * FROM erp_stock_check_item WHERE check_id = #{checkId} AND diff_type != 0 AND deleted = 0")
    List<StockCheckItem> selectDiffItemsByCheckId(@Param("checkId") Long checkId);

    @Select("SELECT COUNT(*) FROM erp_stock_check_item WHERE check_id = #{checkId} AND check_status = 1 AND deleted = 0")
    Integer countCheckedItemsByCheckId(@Param("checkId") Long checkId);

    @Select("SELECT COUNT(*) FROM erp_stock_check_item WHERE check_id = #{checkId} AND diff_type != 0 AND deleted = 0")
    Integer countDiffItemsByCheckId(@Param("checkId") Long checkId);

    @Select("SELECT SUM(book_quantity) FROM erp_stock_check_item WHERE check_id = #{checkId} AND deleted = 0")
    java.math.BigDecimal sumBookQuantityByCheckId(@Param("checkId") Long checkId);

    @Select("SELECT SUM(actual_quantity) FROM erp_stock_check_item WHERE check_id = #{checkId} AND deleted = 0")
    java.math.BigDecimal sumActualQuantityByCheckId(@Param("checkId") Long checkId);

    @Select("SELECT SUM(diff_quantity) FROM erp_stock_check_item WHERE check_id = #{checkId} AND deleted = 0")
    java.math.BigDecimal sumDiffQuantityByCheckId(@Param("checkId") Long checkId);
}