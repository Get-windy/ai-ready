package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 菜单服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 创建菜单
     */
    Long createMenu(SysMenu menu);

    /**
     * 更新菜单
     */
    void updateMenu(SysMenu menu);

    /**
     * 删除菜单
     */
    void deleteMenu(Long menuId);

    /**
     * 获取菜单树
     */
    List<SysMenu> getMenuTree(Long tenantId);

    /**
     * 获取用户菜单树
     */
    List<SysMenu> getUserMenuTree(Long userId);

    /**
     * 获取子菜单
     */
    List<SysMenu> getChildrenMenus(Long parentId, Long tenantId);

    /**
     * 获取菜单详情
     */
    SysMenu getMenuDetail(Long menuId);

    /**
     * 获取所有菜单（平铺列表）
     */
    List<SysMenu> listAllMenus(Long tenantId);

    /**
     * 更新菜单排序
     */
    void updateMenuSort(Long menuId, Integer sort);

    /**
     * 启用/禁用菜单
     */
    void updateMenuStatus(Long menuId, Integer status);

    /**
     * 检查菜单编码是否存在
     */
    boolean checkMenuCodeExists(String menuCode, Long tenantId, Long excludeId);
}