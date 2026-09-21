package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysRolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色-权限关联Mapper
 *
 * <p>⚠️ 本接口**不再声明任何自定义方法**（2026-09-19 修）。原因：原先的
 * {@code batchInsert / deleteByRoleId / deleteByPermissionId / selectPermissionIdsByRoleId}
 * 四个方法**既无注解、也没有对应的 mapper XML**（同目录 SysRoleMenuMapper.xml / SysUserRoleMapper.xml 都有，
 * 唯独没有 SysRolePermissionMapper.xml），调用即抛
 * {@code BindingException: Invalid bound statement (not found)} ——
 * 直接导致「角色 → 设置权限」保存恒 500、该功能**从未真正生效过**。
 * </p>
 * <p>修法沿用本仓既定口径：**删掉自定义方法，改由 Service 层用 {@code LambdaQueryWrapper} 完成**
 * （比补 XML 更好：多租户与逻辑删除才会自动生效）。本表 {@code sys_role_permission} 无 deleted 列，
 * 故删除为物理删除。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
}
