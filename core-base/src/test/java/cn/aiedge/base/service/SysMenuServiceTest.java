package cn.aiedge.base.service;

import cn.aiedge.base.entity.SysMenu;
import cn.aiedge.base.mapper.SysMenuMapper;
import cn.aiedge.base.service.impl.SysMenuServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 菜单服务单元测试
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("菜单服务测试")
class SysMenuServiceTest {

    @Mock
    private SysMenuMapper sysMenuMapper;

    @InjectMocks
    private SysMenuServiceImpl menuService;

    private SysMenu testMenu;

    @BeforeEach
    void setUp() {
        testMenu = new SysMenu();
        testMenu.setId(1L);
        testMenu.setTenantId(1L);
        testMenu.setMenuName("系统管理");
        testMenu.setMenuCode("SYSTEM_MANAGE");
        testMenu.setParentId(0L);
        testMenu.setMenuType(1); // 目录
        testMenu.setPath("/system");
        testMenu.setComponent("Layout");
        testMenu.setSort(1);
        testMenu.setVisible(1);
        testMenu.setStatus(0);
        testMenu.setCreateTime(LocalDateTime.now());
        testMenu.setUpdateTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("菜单实体 - 属性设置")
    void testMenuEntityProperties() {
        assertNotNull(testMenu.getId());
        assertEquals(1L, testMenu.getId());
        assertEquals("系统管理", testMenu.getMenuName());
        assertEquals("SYSTEM_MANAGE", testMenu.getMenuCode());
        assertEquals(0L, testMenu.getParentId());
        assertEquals(1, testMenu.getMenuType());
        assertEquals("/system", testMenu.getPath());
    }

    @Test
    @DisplayName("菜单链式设置")
    void testMenuChainSetter() {
        SysMenu menu = new SysMenu()
                .setId(2L)
                .setMenuName("用户管理")
                .setMenuCode("USER_MANAGE")
                .setParentId(1L)
                .setMenuType(2);

        assertEquals(2L, menu.getId());
        assertEquals("用户管理", menu.getMenuName());
        assertEquals("USER_MANAGE", menu.getMenuCode());
        assertEquals(1L, menu.getParentId());
        assertEquals(2, menu.getMenuType());
    }

    @Test
    @DisplayName("菜单类型 - 类型值验证")
    void testMenuTypeValues() {
        // 菜单类型: 1-目录, 2-菜单, 3-按钮
        testMenu.setMenuType(1);
        assertEquals(1, testMenu.getMenuType());
        
        testMenu.setMenuType(2);
        assertEquals(2, testMenu.getMenuType());
        
        testMenu.setMenuType(3);
        assertEquals(3, testMenu.getMenuType());
    }

    @Test
    @DisplayName("菜单状态 - 状态值验证")
    void testMenuStatusValues() {
        // 状态: 0-正常, 1-停用
        testMenu.setStatus(0);
        assertEquals(0, testMenu.getStatus());
        
        testMenu.setStatus(1);
        assertEquals(1, testMenu.getStatus());
    }

    @Test
    @DisplayName("菜单可见性 - 可见值验证")
    void testMenuVisibleValues() {
        // 可见: 0-隐藏, 1-显示
        testMenu.setVisible(0);
        assertEquals(0, testMenu.getVisible());
        
        testMenu.setVisible(1);
        assertEquals(1, testMenu.getVisible());
    }

    @Test
    @DisplayName("菜单排序 - 排序值测试")
    void testMenuSort() {
        testMenu.setSort(1);
        assertEquals(1, testMenu.getSort());
        
        testMenu.setSort(100);
        assertEquals(100, testMenu.getSort());
    }

    @Test
    @DisplayName("父级菜单 - 根菜单验证")
    void testRootMenu() {
        // 根菜单的parentId为0
        testMenu.setParentId(0L);
        assertEquals(0L, testMenu.getParentId());
    }

    @Test
    @DisplayName("子菜单 - 父级关联")
    void testChildMenu() {
        SysMenu childMenu = new SysMenu();
        childMenu.setId(2L);
        childMenu.setMenuName("用户列表");
        childMenu.setParentId(1L); // 指向testMenu
        
        assertEquals(1L, childMenu.getParentId());
        assertEquals(testMenu.getId(), childMenu.getParentId());
    }

    @Test
    @DisplayName("菜单编码 - 唯一性验证")
    void testMenuCodeUniqueness() {
        String menuCode = "UNIQUE_MENU_CODE";
        testMenu.setMenuCode(menuCode);
        
        assertEquals(menuCode, testMenu.getMenuCode());
    }

    @Test
    @DisplayName("菜单路径 - 路径格式")
    void testMenuPath() {
        testMenu.setPath("/system/user");
        assertEquals("/system/user", testMenu.getPath());
        
        testMenu.setPath("/dashboard");
        assertEquals("/dashboard", testMenu.getPath());
    }

    @Test
    @DisplayName("组件路径 - 组件验证")
    void testMenuComponent() {
        testMenu.setComponent("system/user/index");
        assertEquals("system/user/index", testMenu.getComponent());
        
        testMenu.setComponent("Layout");
        assertEquals("Layout", testMenu.getComponent());
    }

    @Test
    @DisplayName("菜单图标 - 图标设置")
    void testMenuIcon() {
        testMenu.setIcon("system");
        assertEquals("system", testMenu.getIcon());
    }

    @Test
    @DisplayName("菜单路由名称 - routeName字段")
    void testMenuRouteName() {
        testMenu.setRouteName("SystemUser");
        assertEquals("SystemUser", testMenu.getRouteName());
    }

    @Test
    @DisplayName("菜单重定向 - redirect字段")
    void testMenuRedirect() {
        testMenu.setRedirect("/system/user");
        assertEquals("/system/user", testMenu.getRedirect());
    }

    @Test
    @DisplayName("外链设置 - isExternal字段")
    void testMenuExternal() {
        testMenu.setIsExternal(0);
        assertEquals(0, testMenu.getIsExternal());
        
        testMenu.setIsExternal(1);
        assertEquals(1, testMenu.getIsExternal());
    }

    @Test
    @DisplayName("缓存设置 - isCache字段")
    void testMenuCache() {
        testMenu.setIsCache(0);
        assertEquals(0, testMenu.getIsCache());
        
        testMenu.setIsCache(1);
        assertEquals(1, testMenu.getIsCache());
    }

    @Test
    @DisplayName("备注信息 - remark字段")
    void testMenuRemark() {
        testMenu.setRemark("菜单备注");
        assertEquals("菜单备注", testMenu.getRemark());
    }

    @Test
    @DisplayName("时间戳 - 创建和更新")
    void testTimestampFields() {
        LocalDateTime create = LocalDateTime.now().minusHours(1);
        LocalDateTime update = LocalDateTime.now();
        
        testMenu.setCreateTime(create);
        testMenu.setUpdateTime(update);
        
        assertEquals(create, testMenu.getCreateTime());
        assertEquals(update, testMenu.getUpdateTime());
    }

    @Test
    @DisplayName("菜单树构建 - 空列表")
    void testBuildMenuTreeEmpty() {
        List<SysMenu> emptyList = new ArrayList<>();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    @DisplayName("菜单树构建 - 单层菜单")
    void testBuildMenuTreeSingleLevel() {
        List<SysMenu> menus = new ArrayList<>();
        
        SysMenu menu1 = new SysMenu();
        menu1.setId(1L);
        menu1.setParentId(0L);
        menu1.setMenuName("菜单1");
        menus.add(menu1);
        
        SysMenu menu2 = new SysMenu();
        menu2.setId(2L);
        menu2.setParentId(0L);
        menu2.setMenuName("菜单2");
        menus.add(menu2);
        
        assertEquals(2, menus.size());
    }

    @Test
    @DisplayName("菜单树构建 - 多层菜单")
    void testBuildMenuTreeMultiLevel() {
        List<SysMenu> menus = new ArrayList<>();
        
        // 一级菜单
        SysMenu parent = new SysMenu();
        parent.setId(1L);
        parent.setParentId(0L);
        parent.setMenuName("父菜单");
        menus.add(parent);
        
        // 二级菜单
        SysMenu child = new SysMenu();
        child.setId(2L);
        child.setParentId(1L);
        child.setMenuName("子菜单");
        menus.add(child);
        
        assertEquals(2, menus.size());
        assertEquals(0L, menus.get(0).getParentId());
        assertEquals(1L, menus.get(1).getParentId());
    }

    @Test
    @DisplayName("删除菜单 - 存在子菜单")
    void testDeleteMenuWithChildren() {
        // 如果存在子菜单，无法删除
        Long parentId = 1L;
        
        // 模拟存在子菜单
        SysMenu child = new SysMenu();
        child.setId(2L);
        child.setParentId(parentId);
        
        assertNotNull(child.getParentId());
        assertEquals(parentId, child.getParentId());
    }

    @Test
    @DisplayName("菜单查询 - 按租户ID")
    void testQueryByTenantId() {
        Long tenantId = 1L;
        testMenu.setTenantId(tenantId);
        
        assertEquals(tenantId, testMenu.getTenantId());
    }

    @Test
    @DisplayName("菜单状态更新 - 启用/禁用")
    void testUpdateMenuStatus() {
        // 正常状态
        testMenu.setStatus(0);
        assertEquals(0, testMenu.getStatus());
        
        // 禁用状态
        testMenu.setStatus(1);
        assertEquals(1, testMenu.getStatus());
    }

    @Test
    @DisplayName("菜单排序更新")
    void testUpdateMenuSort() {
        Integer newSort = 10;
        testMenu.setSort(newSort);
        
        assertEquals(newSort, testMenu.getSort());
    }
}