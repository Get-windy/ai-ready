package com.aiready.menu;

import com.aiready.menu.dto.*;
import com.aiready.menu.entity.Menu;
import com.aiready.menu.service.MenuService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MenuService menuService;

    @Test
    void testCreateAndDeleteMenu() throws Exception {
        // 准备创建请求
        MenuSaveRequest request = new MenuSaveRequest();
        request.setParentId(0L);
        request.setMenuName("测试菜单");
        request.setMenuCode("test:menu:" + System.currentTimeMillis());
        request.setMenuType(2); // 菜单类型
        request.setPath("/test/menu");
        request.setComponent("/test/menu/index");

        // 测试创建菜单
        String requestBody = objectMapper.writeValueAsString(request);
        mockMvc.perform(post("/api/menu/create?operatorId=1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        // 创建后需要获取菜单ID进行删除测试
        // 这里我们先手动创建并获取ID
        MenuDTO createdMenu = menuService.createMenu(request, 1L);
        Long menuId = createdMenu.getId();
        assertNotNull(menuId);

        // 测试删除菜单
        mockMvc.perform(delete("/api/menu/delete/" + menuId + "?operatorId=1"))
                .andExpect(status().isOk());

        // 验证菜单已删除
        MenuDTO deletedMenu = menuService.getMenuById(menuId);
        assertNull(deletedMenu);
    }

    @Test
    void testGetMenuTree() throws Exception {
        MenuQueryRequest request = new MenuQueryRequest();
        request.setBuildTree(true);
        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/menu/tree")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllMenus() throws Exception {
        mockMvc.perform(get("/api/menu/all"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateMenu() throws Exception {
        // 先创建一个菜单
        MenuSaveRequest createRequest = new MenuSaveRequest();
        createRequest.setParentId(0L);
        createRequest.setMenuName("原始菜单");
        createRequest.setMenuCode("original:menu:" + System.currentTimeMillis());
        createRequest.setMenuType(2);
        createRequest.setPath("/original/path");
        createRequest.setComponent("/original/component");

        MenuDTO createdMenu = menuService.createMenu(createRequest, 1L);
        Long menuId = createdMenu.getId();
        assertNotNull(menuId);

        // 准备更新请求
        MenuUpdateRequest updateRequest = new MenuUpdateRequest();
        updateRequest.setId(menuId);
        updateRequest.setParentId(0L);
        updateRequest.setMenuName("更新后的菜单");
        updateRequest.setMenuType(2);
        updateRequest.setPath("/updated/path");
        updateRequest.setComponent("/updated/component");

        String requestBody = objectMapper.writeValueAsString(updateRequest);

        mockMvc.perform(put("/api/menu/update?operatorId=1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        // 清理数据
        menuService.deleteMenu(menuId, 1L);
    }

    @Test
    void testValidateMenuCode() throws Exception {
        String testCode = "test:validate:" + System.currentTimeMillis();
        mockMvc.perform(get("/api/menu/validate-code?menuCode=" + testCode))
                .andExpect(status().isOk());
    }
}