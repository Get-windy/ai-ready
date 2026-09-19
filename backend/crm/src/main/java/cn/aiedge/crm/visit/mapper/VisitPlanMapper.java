package cn.aiedge.crm.visit.mapper;

import cn.aiedge.crm.visit.entity.VisitPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VisitPlanMapper extends BaseMapper<VisitPlan> {

    /**
     * 当日拜访计划单号的最大值。刻意不带 deleted 条件：唯一索引 uk_crm_visit_plan_no
     * 不含 deleted，已逻辑删除的行仍占号，取号时必须把它们算进来（否则会撞索引）。
     */
    @Select("SELECT MAX(plan_no) FROM crm_visit_plan WHERE plan_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxPlanNo(@Param("prefix") String prefix);
}
