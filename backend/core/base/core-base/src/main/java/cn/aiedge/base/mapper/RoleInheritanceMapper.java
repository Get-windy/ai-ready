package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.RoleInheritance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色继承关系Mapper接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface RoleInheritanceMapper extends BaseMapper<RoleInheritance> {
    
    /**
     * 删除角色的继承关系
     */
    void deleteByRoleId(Long roleId);
}