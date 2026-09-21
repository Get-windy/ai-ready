package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysModulePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 「模块 → 权限码前缀」映射 Mapper。
 *
 * @author AI-Ready Team
 * @since 0.3.22
 */
@Mapper
public interface SysModulePermissionMapper extends BaseMapper<SysModulePermission> {
}
