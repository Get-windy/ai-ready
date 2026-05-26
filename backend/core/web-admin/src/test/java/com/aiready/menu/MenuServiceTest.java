package com.aiready.menu;

import com.aiready.menu.dto.*;
import com.aiready.menu.entity.Menu;
import com.aiready.menu.service.MenuService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class MenuServiceTest {

    @Resource
    private MenuService menuService;

    @Test
    void testCreateAndDeleteMenu() {
        // 创建菜单
        MenuSaveRequest request = new MenuSaveRequest();
        request.setParentId(0L);
        request.setMenuName("测试菜单");
        request.setMenuCode("test:menu");
        request.setMenuType(2); // 菜单类型
        request.setPath("/test/menu");
        request.setComponent("/test/menu/index");

        MenuDTO createdMenu = menuService.createMenu(request, 1L);
        assertNotNull(createdMenu);
        assertEquals("测试菜单", createdMenu.getMenuName());

        // 删除菜单
        menuService.deleteMenu(createdMenu.getId(), 1L);

        // 验证菜单已删除
        MenuDTO deletedMenu = menuService.getMenuById(createdMenu.getId());
        assertNull(deletedMenu);
    }

    @Test
    void testGetAllMenus() {
        List<MenuDTO> menus = menuService.getAllMenus();
        assertNotNull(menus);
        assertTrue(menus.size() >= 0);
    }

    @Test
    void testGetMenuTree() {
        MenuQueryRequest request = new MenuQueryRequest();
        request.setBuildTree(true);
        List<MenuDTO> tree = menuService.getMenuTree(request);
        assertNotNull(tree);
    }

    @Test
    void testValidateMenuCode() {
        String testCode = "test:unique:code:" + System.currentTimeMillis();
        boolean isValid = menuService.validateMenuCode(testCode, null);
        assertTrue(isValid);

        // 测试创建一个菜单后再验证相同编码
        MenuSaveRequest request = new MenuSaveRequest();
        request.setParentId(0L);
        request.setMenuName("测试验证菜单");
        request.setMenuCode(testCode);
        request.setMenuType(2);
        request.setPath("/test/validate");
        request.setComponent("/test/validate/index");

        MenuDTO createdMenu = menuService.createMenu(request, 1L);
        assertNotNull(createdMenu);

        // 验证相同编码应该返回false
        boolean isInvalid = menuService.validateMenuCode(testCode, null);
        assertFalse(isInvalid);

        // 验证相同编码但排除当前ID应该返回true
        boolean isValidWithExclude = menuService.validateMenuCode(testCode, createdMenu.getId());
        assertTrue(isValidWithExclude);

        // 清理
        menuService.deleteMenu(createdMenu.getId(), 1L);
    }

    @Test
    void testUpdateMenu() {
        // 先创建一个菜单
        MenuSaveRequest createRequest = new MenuSaveRequest();
        createRequest.setParentId(0L);
        createRequest.setMenuName("原始菜单");
        createRequest.setMenuCode("original:menu:" + System.currentTimeMillis());
        createRequest.setMenuType(2);
        createRequest.setPath("/original/path");
        createRequest.setComponent("/original/component");

        MenuDTO createdMenu = menuService.createMenu(createRequest, 1L);
        assertNotNull(createdMenu);

        // 更新菜单
        MenuUpdateRequest updateRequest = new MenuUpdateRequest();
        updateRequest.setId(createdMenu.getId());
        updateRequest.setParentId(0L);
        updateRequest.setMenuName("更新后的菜单");
        updateRequest.setMenuType(2);
        updateRequest.setPath("/updated/path");
        updateRequest.setComponent("/updated/component");

        MenuDTO updatedMenu = menuService.updateMenu(updateRequest, 1L);
        assertNotNull(updatedMenu);
        assertEquals("更新后的菜单", updatedMenu.getMenuName());

        // 清理
        menuService.deleteMenu(updatedMenu.getId(), 1L);
    }
}