package cn.aiedge.dict.controller;

import cn.aiedge.dict.service.DictItemService;
import cn.aiedge.dict.service.DictTypeService;
import cn.aiedge.dict.vo.DictItemVO;
import cn.aiedge.dict.vo.DictTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典查询控制器
 * 提供前端下拉选择器所需的数据格式
 */
@Slf4j
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
@Tag(name = "字典查询", description = "字典数据查询接口，供前端下拉选择器使用")
public class DictController {

    private final DictTypeService dictTypeService;
    private final DictItemService dictItemService;

    @GetMapping("/{dictCode}")
    @Operation(summary = "根据字典编码获取字典选项")
    public DictOption getByCode(@PathVariable String dictCode) {
        DictTypeVO dictType = dictTypeService.getByDictCode(dictCode);
        if (dictType == null) {
            DictOption empty = new DictOption();
            empty.setDictCode(dictCode);
            empty.setDictName("");
            empty.setItems(new ArrayList<>());
            return empty;
        }

        List<DictItemVO> items = dictItemService.getByDictCode(dictCode);

        DictOption result = new DictOption();
        result.setDictCode(dictType.getDictCode());
        result.setDictName(dictType.getDictName());
        result.setItems(items.stream()
                .filter(i -> "ENABLED".equals(i.getStatus()) || i.getStatus() == null)
                .map(i -> {
                    DictOption.Item item = new DictOption.Item();
                    item.setValue(i.getItemValue());
                    item.setLabel(i.getItemText());
                    return item;
                })
                .collect(Collectors.toList()));
        return result;
    }

    @PostMapping("/batch")
    @Operation(summary = "批量获取字典选项")
    public List<DictOption> getBatch(@RequestBody Map<String, List<String>> request) {
        List<String> dictCodes = request.get("dictCodes");
        if (dictCodes == null || dictCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return dictCodes.stream()
                .map(this::getByCode)
                .collect(Collectors.toList());
    }

    /**
     * 字典选项DTO，匹配前端 DictOption 接口
     */
    @lombok.Data
    public static class DictOption {
        private String dictCode;
        private String dictName;
        private List<Item> items;

        @lombok.Data
        public static class Item {
            private String value;
            private String label;
        }
    }
}
