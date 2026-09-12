package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.CustomerGradeDiscount;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 客户级别折扣设置 Mapper
 */
@Mapper
public interface CustomerGradeDiscountMapper extends BaseMapper<CustomerGradeDiscount> {

    /** 最后修改人姓名：与系统其它页面同一口径（real_name → nickname → username） */
    @Select("<script>SELECT u.id AS id, COALESCE(u.real_name, u.nickname, u.username) AS name"
        + " FROM sys_user u WHERE u.deleted = 0 AND u.id IN"
        + " <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
        + "</script>")
    List<Map<String, Object>> selectModifierNames(@Param("ids") List<Long> ids);
}
