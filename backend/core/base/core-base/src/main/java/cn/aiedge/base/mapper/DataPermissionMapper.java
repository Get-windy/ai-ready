package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.DataPermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据权限Mapper接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface DataPermissionMapper extends BaseMapper<DataPermission> {
    
    /**
     * 根据角色ID列表查询数据权限
     */
    List<DataPermission> selectDataPermissionsByRoleIds(@Param("roleIds") List<Long> roleIds);
}