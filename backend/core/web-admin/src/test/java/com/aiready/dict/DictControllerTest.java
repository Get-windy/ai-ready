package com.aiready.dict;

import com.aiready.dict.dto.*;
import com.aiready.dict.service.DictService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class DictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DictService dictService;

    @Test
    void testCreateAndDeleteDictType() throws Exception {
        // 准备创建请求
        DictSaveRequest request = new DictSaveRequest();
        request.setDictTypeCode("test:dict:" + System.currentTimeMillis());
        request.setDictTypeName("测试字典类型");
        request.setDescription("这是一个测试字典类型");

        DictSaveRequest.DictItemRequest item1 = new DictSaveRequest.DictItemRequest();
        item1.setDictItemCode("test_item_1");
        item1.setDictItemName("测试项1");
        item1.setDictItemValue("TEST_VALUE_1");
        item1.setDictItemLabel("测试项1标签");

        DictSaveRequest.DictItemRequest item2 = new DictSaveRequest.DictItemRequest();
        item2.setDictItemCode("test_item_2");
        item2.setDictItemName("测试项2");
        item2.setDictItemValue("TEST_VALUE_2");
        item2.setDictItemLabel("测试项2标签");

        request.setDictItems(Arrays.asList(item1, item2));

        String requestBody = objectMapper.writeValueAsString(request);

        // 测试创建字典类型
        mockMvc.perform(post("/api/dict/create-type?operatorId=1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        // 手动创建以便获取ID进行删除测试
        DictTypeDTO createdType = dictService.createDictType(request, 1L);
        Long dictTypeId = createdType.getId();
        assertNotNull(dictTypeId);

        // 测试删除字典类型
        mockMvc.perform(delete("/api/dict/delete-type/" + dictTypeId + "?operatorId=1"))
                .andExpect(status().isOk());

        // 验证已删除
        DictTypeDTO deletedType = dictService.getDictTypeById(dictTypeId);
        assertNull(deletedType);
    }

    @Test
    void testGetDictTypeByCode() throws Exception {
        mockMvc.perform(get("/api/dict/type/code/gender"))
                .andExpect(status().isOk());
    }

    @Test
    void testQueryDictTypes() throws Exception {
        DictQueryRequest request = new DictQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setOnlyEnabled(true);

        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/dict/types")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllDictTypes() throws Exception {
        mockMvc.perform(get("/api/dict/types/all"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetDictItemsByTypeCode() throws Exception {
        mockMvc.perform(get("/api/dict/items/type-code/gender"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetDictItemByTypeCodeAndValue() throws Exception {
        mockMvc.perform(get("/api/dict/item/value/gender/M"))
                .andExpect(status().isOk());
    }

    @Test
    void testValidateDictTypeCode() throws Exception {
        String testCode = "test:validate:code:" + System.currentTimeMillis();
        mockMvc.perform(get("/api/dict/validate/type-code?dictTypeCode=" + testCode))
                .andExpect(status().isOk());
    }

    @Test
    void testRefreshAndClearCache() throws Exception {
        mockMvc.perform(post("/api/dict/refresh-cache"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/dict/clear-cache"))
                .andExpect(status().isOk());
    }
}