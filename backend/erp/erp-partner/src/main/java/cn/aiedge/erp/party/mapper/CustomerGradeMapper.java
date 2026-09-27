package cn.aiedge.erp.party.mapper;

import cn.aiedge.erp.party.entity.CustomerGrade;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CustomerGradeMapper extends BaseMapper<CustomerGrade> {

    /**
     * 统计正在使用该级别名称的往来单位数（引用保护用）。
     * 客户档案按级别名称关联（biz_party.party_level），故按名称统计。
     */
    @Select("SELECT COUNT(*) FROM biz_party WHERE party_level = #{gradeName} AND deleted = 0")
    Long countUsedByGradeName(@Param("gradeName") String gradeName);
}
