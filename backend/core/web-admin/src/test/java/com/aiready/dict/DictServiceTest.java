package com.aiready.dict;

import com.aiready.dict.dto.*;
import com.aiready.dict.entity.DictType;
import com.aiready.dict.service.DictService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class DictServiceTest {

    @Resource
    private DictService dictService;

    @Test
    void testCreateAndDeleteDictType() {
        // 准备测试数据
        DictSaveRequest request = new DictSaveRequest();
        request.setDictTypeCode("test:type:" + System.currentTimeMillis());
        request.setDictTypeName("测试字典类型");
        request.setDescription("这是一个测试字典类型");
        request.setStatus(1);
        request.setSortOrder(0);

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

        // 创建字典类型
        DictTypeDTO createdType = dictService.createDictType(request, 1L);
        assertNotNull(createdType);
        assertEquals("测试字典类型", createdType.getDictTypeName());
        assertEquals(2, createdType.getDictItems().size());

        // 验证字典项
        List<DictItemDTO> items = dictService.getDictItemsByTypeId(createdType.getId());
        assertEquals(2, items.size());

        // 删除字典类型
        dictService.deleteDictType(createdType.getId(), 1L);

        // 验证已删除
        DictTypeDTO deletedType = dictService.getDictTypeById(createdType.getId());
        assertNull(deletedType);
    }

    @Test
    void testGetDictTypeByCode() {
        DictTypeDTO dictType = dictService.getDictTypeByCode("gender");
        assertNotNull(dictType);
        assertEquals("性别", dictType.getDictTypeName());
    }

    @Test
    void testQueryDictTypes() {
        DictQueryRequest request = new DictQueryRequest();
        request.setPageNum(1);
        request.setPageSize(10);
        request.setOnlyEnabled(true);

        IPage<DictTypeDTO> page = dictService.queryDictTypes(request);
        assertNotNull(page);
        assertTrue(page.getTotal() >= 0);
    }

    @Test
    void testGetDictItemsByTypeCode() {
        List<DictItemDTO> items = dictService.getDictItemsByTypeCode("gender");
        assertNotNull(items);
        assertTrue(items.size() > 0);
    }

    @Test
    void testGetDictItemByTypeCodeAndValue() {
        DictItemDTO item = dictService.getDictItemByTypeCodeAndValue("gender", "M");
        assertNotNull(item);
        assertEquals("男", item.getDictItemName());
    }

    @Test
    void testValidateDictTypeCode() {
        String testCode = "test:validate:code:" + System.currentTimeMillis();
        boolean isValid = dictService.validateDictTypeCode(testCode, null);
        assertTrue(isValid);
    }

    @Test
    void testUpdateDictTypeStatus() {
        // 先创建一个测试字典类型
        DictSaveRequest request = new DictSaveRequest();
        request.setDictTypeCode("test:update:status:" + System.currentTimeMillis());
        request.setDictTypeName("状态测试字典");
        request.setDescription("状态测试");
        request.setStatus(1);

        DictTypeDTO createdType = dictService.createDictType(request, 1L);
        assertNotNull(createdType);

        // 更新状态
        dictService.updateDictTypeStatus(createdType.getId(), 0, 1L);

        // 验证状态已更改
        DictTypeDTO updatedType = dictService.getDictTypeById(createdType.getId());
        assertEquals(Integer.valueOf(0), updatedType.getStatus());

        // 清理数据
        dictService.deleteDictType(createdType.getId(), 1L);
    }
}