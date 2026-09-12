package cn.aiedge.erp.stock.mapper;

import cn.aiedge.erp.stock.entity.ProductGrade;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 产品等级Mapper
 *
 * <p>价格等级是**全局标准槽位** GRADE_1..GRADE_8（种子数据 tenant_id=0），
 * 用户只自定义其昵称 grade_name。多租户插件会给 BaseMapper 的查询/更新自动追加
 * tenant_id = 当前租户，导致 tenant_id=0 的字典查不到、昵称也改不动，
 * 因此这里提供显式忽略租户过滤的方法。</p>
 */
public interface ProductGradeMapper extends BaseMapper<ProductGrade> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM erp_product_grade WHERE deleted = 0 AND status = 1 "
            + "ORDER BY sort_order ASC, grade_level ASC")
    List<ProductGrade> selectActiveGradesIgnoreTenant();

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM erp_product_grade WHERE deleted = 0 "
            + "ORDER BY sort_order ASC, grade_level ASC")
    List<ProductGrade> selectAllIgnoreTenant();

    @InterceptorIgnore(tenantLine = "true")
    @Update("<script>UPDATE erp_product_grade <set>"
            + "<if test='gradeName != null'>grade_name = #{gradeName},</if>"
            + "<if test='gradeLevel != null'>grade_level = #{gradeLevel},</if>"
            + "<if test='sortOrder != null'>sort_order = #{sortOrder},</if>"
            + "<if test='status != null'>status = #{status},</if>"
            + "<if test='remark != null'>remark = #{remark},</if>"
            + "<if test='description != null'>description = #{description},</if>"
            + "update_time = now()</set> WHERE id = #{id} AND deleted = 0</script>")
    int updateIgnoreTenant(ProductGrade grade);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE erp_product_grade SET deleted = 1, update_time = now() WHERE id = #{id} AND deleted = 0")
    int deleteIgnoreTenant(@Param("id") Long id);
}
