package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysSodRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 职责分离规则 Mapper
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysSodRuleMapper extends BaseMapper<SysSodRule> {

    /**
     * 查询所有启用的 SoD 规则
     */
    List<SysSodRule> selectActiveRules();

    /**
     * 根据角色ID查询包含该角色的所有 SoD 规则
     */
    List<SysSodRule> selectByRoleId(@Param("roleId") Long roleId);
}
