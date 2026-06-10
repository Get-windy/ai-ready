package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.Partner;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

public interface PartnerMapper extends BaseMapper<Partner> {

    @Select("SELECT p.*, pc.category_name, pg.grade_name " +
            "FROM erp_partner p " +
            "LEFT JOIN erp_partner_category pc ON p.partner_category_id = pc.id AND pc.deleted = 0 " +
            "LEFT JOIN erp_partner_grade pg ON p.partner_grade_id = pg.id AND pg.deleted = 0 " +
            "${ew.customSqlSegment}")
    @Results({
            @Result(column = "category_name", property = "categoryName"),
            @Result(column = "grade_name", property = "gradeName")
    })
    IPage<Partner> selectPartnerPage(IPage<Partner> page, @Param(Constants.WRAPPER) Wrapper<Partner> wrapper);

    @Select("SELECT p.*, pc.category_name, pg.grade_name " +
            "FROM erp_partner p " +
            "LEFT JOIN erp_partner_category pc ON p.partner_category_id = pc.id AND pc.deleted = 0 " +
            "LEFT JOIN erp_partner_grade pg ON p.partner_grade_id = pg.id AND pg.deleted = 0 " +
            "WHERE p.id = #{id} AND p.deleted = 0")
    @Results({
            @Result(column = "category_name", property = "categoryName"),
            @Result(column = "grade_name", property = "gradeName")
    })
    Partner selectPartnerDetail(@Param("id") Long id);
}
