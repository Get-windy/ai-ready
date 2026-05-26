package com.aiready.dict.service.impl;

import com.aiready.dict.dto.*;
import com.aiready.dict.entity.DictItem;
import com.aiready.dict.entity.DictType;
import com.aiready.dict.mapper.DictItemMapper;
import com.aiready.dict.mapper.DictTypeMapper;
import com.aiready.dict.service.DictService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 字典服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictServiceImpl extends ServiceImpl<DictTypeMapper, DictType> implements DictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional
    public DictTypeDTO createDictType(DictSaveRequest request, Long operatorId) {
        // 验证字典类型编码唯一性
        if (!validateDictTypeCode(request.getDictCode(), null)) {
            throw new RuntimeException("字典类型编码已存在：" + request.getDictCode());
        }

        // 创建字典类型
        DictType dictType = new DictType();
        dictType.setDictCode(request.getDictCode());
        dictType.setDictTypeName(request.getDictTypeName());
        dictType.setDescription(request.getDescription());
        dictType.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        dictType.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        dictType.setRemark(request.getRemark());
        dictType.setCreateBy(operatorId);
        dictType.setUpdateBy(operatorId);

        dictTypeMapper.insert(dictType);

        // 创建字典项
        if (request.getDictItems() != null && !request.getDictItems().isEmpty()) {
            List<DictItem> dictItems = request.getDictItems().stream().map(itemReq -> {
                DictItem item = new DictItem();
                item.setDictTypeId(dictType.getId());
                item.setDictItemCode(itemReq.getDictItemCode());
                item.setDictItemName(itemReq.getDictItemName());
                item.setDictItemValue(itemReq.getDictItemValue());
                item.setDictItemLabel(itemReq.getDictItemLabel());
                item.setStatus(itemReq.getStatus() != null ? itemReq.getStatus() : 1);
                item.setSortOrder(itemReq.getSortOrder() != null ? itemReq.getSortOrder() : 0);
                item.setColorStyle(itemReq.getColorStyle());
                item.setIcon(itemReq.getIcon());
                item.setRemark(itemReq.getRemark());
                item.setCreateBy(operatorId);
                item.setUpdateBy(operatorId);
                return item;
            }).collect(Collectors.toList());

            // 批量插入字典项
            dictItemMapper.batchInsert(dictItems);
        }

        // 刷新缓存
        refreshCache();

        return getDictTypeById(dictType.getId());
    }

    @Override
    @Transactional
    public DictTypeDTO updateDictType(Long dictTypeId, DictSaveRequest request, Long operatorId) {
        // 验证字典类型编码唯一性
        if (!validateDictTypeCode(request.getDictCode(), dictTypeId)) {
            throw new RuntimeException("字典类型编码已存在：" + request.getDictCode());
        }

        // 更新字典类型
        DictType dictType = dictTypeMapper.selectById(dictTypeId);
        if (dictType == null) {
            throw new RuntimeException("字典类型不存在，ID：" + dictTypeId);
        }

        dictType.setDictCode(request.getDictCode());
        dictType.setDictTypeName(request.getDictTypeName());
        dictType.setDescription(request.getDescription());
        dictType.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        dictType.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        dictType.setRemark(request.getRemark());
        dictType.setUpdateBy(operatorId);
        dictType.setUpdateTime(LocalDateTime.now());

        dictTypeMapper.updateById(dictType);

        // 删除原有字典项
        dictItemMapper.deleteByDictTypeId(dictTypeId);

        // 创建新的字典项
        if (request.getDictItems() != null && !request.getDictItems().isEmpty()) {
            List<DictItem> dictItems = request.getDictItems().stream().map(itemReq -> {
                DictItem item = new DictItem();
                item.setDictTypeId(dictType.getId());
                item.setDictItemCode(itemReq.getDictItemCode());
                item.setDictItemName(itemReq.getDictItemName());
                item.setDictItemValue(itemReq.getDictItemValue());
                item.setDictItemLabel(itemReq.getDictItemLabel());
                item.setStatus(itemReq.getStatus() != null ? itemReq.getStatus() : 1);
                item.setSortOrder(itemReq.getSortOrder() != null ? itemReq.getSortOrder() : 0);
                item.setColorStyle(itemReq.getColorStyle());
                item.setIcon(itemReq.getIcon());
                item.setRemark(itemReq.getRemark());
                item.setCreateBy(operatorId);
                item.setUpdateBy(operatorId);
                return item;
            }).collect(Collectors.toList());

            // 批量插入字典项
            dictItemMapper.batchInsert(dictItems);
        }

        // 刷新缓存
        refreshCache();

        return getDictTypeById(dictTypeId);
    }

    @Override
    @Transactional
    public void deleteDictType(Long dictTypeId, Long operatorId) {
        DictType dictType = dictTypeMapper.selectById(dictTypeId);
        if (dictType != null) {
            // 删除字典项
            dictItemMapper.deleteByDictTypeId(dictTypeId);

            // 删除字典类型
            dictTypeMapper.deleteById(dictTypeId);

            // 刷新缓存
            refreshCache();
        }
    }

    @Override
    @Transactional
    public void batchDeleteDictTypes(List<Long> dictTypeIds, Long operatorId) {
        for (Long dictTypeId : dictTypeIds) {
            deleteDictType(dictTypeId, operatorId);
        }
    }

    @Override
    public DictTypeDTO getDictTypeById(Long dictTypeId) {
        DictType dictType = dictTypeMapper.selectById(dictTypeId);
        if (dictType == null) {
            return null;
        }

        DictTypeDTO dto = convertToDictTypeDTO(dictType);

        // 获取字典项
        List<DictItem> dictItems = dictItemMapper.selectByDictTypeId(dictTypeId);
        dto.setDictItems(dictItems.stream().map(this::convertToDictItemDTO).collect(Collectors.toList()));

        return dto;
    }

    @Override
    public DictTypeDTO getDictTypeByCode(String dictTypeCode) {
        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictType::getDictTypeCode, dictTypeCode);
        wrapper.eq(DictType::getDeleted, 0);

        DictType dictType = dictTypeMapper.selectOne(wrapper);
        if (dictType == null) {
            return null;
        }

        return getDictTypeById(dictType.getId());
    }

    @Override
    public IPage<DictTypeDTO> queryDictTypes(DictQueryRequest request) {
        Page<DictType> page = new Page<>(request.getPageNum(), request.getPageSize());

        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<>();
        if (request.getDictCode() != null && !request.getDictCode().isEmpty()) {
            wrapper.like(DictType::getDictTypeCode, request.getDictCode());
        }
        if (request.getDictTypeName() != null && !request.getDictTypeName().isEmpty()) {
            wrapper.like(DictType::getDictTypeName, request.getDictTypeName());
        }
        if (request.getStatus() != null) {
            wrapper.eq(DictType::getStatus, request.getStatus());
        }
        if (request.getOnlyEnabled()) {
            wrapper.eq(DictType::getStatus, 1);
        }
        wrapper.eq(DictType::getDeleted, 0);
        wrapper.orderByAsc(DictType::getSortOrder).orderByAsc(DictType::getId);

        IPage<DictType> dictTypePage = dictTypeMapper.selectPage(page, wrapper);

        // 转换为DTO
        IPage<DictTypeDTO> resultPage = dictTypePage.convert(this::convertToDictTypeDTO);

        // 如果需要包含字典项，则加载字典项
        if (request.getIncludeItems()) {
            for (DictTypeDTO dictTypeDTO : resultPage.getRecords()) {
                List<DictItem> dictItems = dictItemMapper.selectByDictTypeId(dictTypeDTO.getId());
                dictTypeDTO.setDictItems(dictItems.stream().map(this::convertToDictItemDTO).collect(Collectors.toList()));
            }
        }

        return resultPage;
    }

    @Override
    public List<DictTypeDTO> getAllDictTypes() {
        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictType::getStatus, 1); // 只获取启用的
        wrapper.eq(DictType::getDeleted, 0);
        wrapper.orderByAsc(DictType::getSortOrder).orderByAsc(DictType::getId);

        List<DictType> dictTypes = dictTypeMapper.selectList(wrapper);
        return dictTypes.stream().map(this::convertToDictTypeDTO).collect(Collectors.toList());
    }

    @Override
    public List<DictItemDTO> getDictItemsByTypeId(Long dictTypeId) {
        List<DictItem> dictItems = dictItemMapper.selectByDictTypeId(dictTypeId);
        return dictItems.stream().map(this::convertToDictItemDTO).collect(Collectors.toList());
    }

    @Override
    public List<DictItemDTO> getDictItemsByTypeCode(String dictTypeCode) {
        List<DictItem> dictItems = dictItemMapper.selectByDictTypeCode(dictTypeCode);
        return dictItems.stream().map(this::convertToDictItemDTO).collect(Collectors.toList());
    }

    @Override
    public DictItemDTO getDictItemByTypeCodeAndValue(String dictTypeCode, String dictItemValue) {
        DictItem dictItem = dictItemMapper.selectByTypeCodeAndValue(dictTypeCode, dictItemValue);
        return dictItem != null ? convertToDictItemDTO(dictItem) : null;
    }

    @Override
    public DictItemDTO getDictItemByTypeCodeAndCode(String dictTypeCode, String dictItemCode) {
        DictItem dictItem = dictItemMapper.selectByTypeCodeAndCode(dictTypeCode, dictItemCode);
        return dictItem != null ? convertToDictItemDTO(dictItem) : null;
    }

    @Override
    public DictItemDTO getDictItemByTypeIdAndValue(Long dictTypeId, String dictItemValue) {
        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItem::getDictTypeId, dictTypeId);
        wrapper.eq(DictItem::getDictItemValue, dictItemValue);
        wrapper.eq(DictItem::getDeleted, 0);

        DictItem dictItem = dictItemMapper.selectOne(wrapper);
        return dictItem != null ? convertToDictItemDTO(dictItem) : null;
    }

    @Override
    public DictItemDTO getDictItemByTypeIdAndCode(Long dictTypeId, String dictItemCode) {
        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DictItem::getDictTypeId, dictTypeId);
        wrapper.eq(DictItem::getDictItemCode, dictItemCode);
        wrapper.eq(DictItem::getDeleted, 0);

        DictItem dictItem = dictItemMapper.selectOne(wrapper);
        return dictItem != null ? convertToDictItemDTO(dictItem) : null;
    }

    @Override
    @Transactional
    public void updateDictItemStatus(Long dictItemId, Integer status, Long operatorId) {
        DictItem dictItem = dictItemMapper.selectById(dictItemId);
        if (dictItem != null) {
            dictItem.setStatus(status);
            dictItem.setUpdateBy(operatorId);
            dictItem.setUpdateTime(LocalDateTime.now());
            dictItemMapper.updateById(dictItem);

            // 刷新缓存
            refreshCache();
        }
    }

    @Override
    @Transactional
    public void updateDictTypeStatus(Long dictTypeId, Integer status, Long operatorId) {
        DictType dictType = dictTypeMapper.selectById(dictTypeId);
        if (dictType != null) {
            dictType.setStatus(status);
            dictType.setUpdateBy(operatorId);
            dictType.setUpdateTime(LocalDateTime.now());
            dictTypeMapper.updateById(dictType);

            // 刷新缓存
            refreshCache();
        }
    }

    @Override
    public boolean validateDictTypeCode(String dictTypeCode, Long excludeId) {
        Integer count = dictTypeMapper.checkDictTypeCodeExists(dictTypeCode, excludeId);
        return count == 0;
    }

    @Override
    public boolean validateDictItemCode(Long dictTypeId, String dictItemCode, Long excludeId) {
        Integer count = dictItemMapper.checkDictItemCodeExists(dictTypeId, dictItemCode, excludeId);
        return count == 0;
    }

    @Override
    public String exportDictData(DictQueryRequest request) {
        // TODO: 实现导出逻辑
        return "dict_export_" + System.currentTimeMillis() + ".xlsx";
    }

    @Override
    public void importDictData(String filePath, Long operatorId) {
        // TODO: 实现导入逻辑
    }

    @Override
    public void refreshCache() {
        // 从数据库加载所有启用的字典数据到缓存
        List<DictTypeDTO> allDictTypes = getAllDictTypes();
        for (DictTypeDTO dictType : allDictTypes) {
            String cacheKey = "dict:type:" + dictType.getDictCode();
            redisTemplate.opsForValue().set(cacheKey, dictType, 24, TimeUnit.HOURS);
            
            // 为每个字典项也创建缓存
            if (dictType.getDictItems() != null) {
                for (DictItemDTO dictItem : dictType.getDictItems()) {
                    String valueCacheKey = "dict:value:" + dictType.getDictCode() + ":" + dictItem.getDictItemValue();
                    String codeCacheKey = "dict:code:" + dictType.getDictCode() + ":" + dictItem.getDictItemCode();
                    
                    redisTemplate.opsForValue().set(valueCacheKey, dictItem, 24, TimeUnit.HOURS);
                    redisTemplate.opsForValue().set(codeCacheKey, dictItem, 24, TimeUnit.HOURS);
                }
            }
        }
    }

    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheEnabled", true);
        // TODO: 实现具体的缓存统计逻辑
        return stats;
    }

    @Override
    public void clearCache() {
        // 清除所有字典相关缓存
        Set<String> keys = redisTemplate.keys("dict:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * 转换字典类型为DTO
     */
    private DictTypeDTO convertToDictTypeDTO(DictType dictType) {
        DictTypeDTO dto = new DictTypeDTO();
        dto.setId(dictType.getId());
        dto.setDictCode(dictType.getDictCode());
        dto.setDictTypeName(dictType.getDictTypeName());
        dto.setDescription(dictType.getDescription());
        dto.setStatus(dictType.getStatus());
        dto.setStatusDesc(dictType.getStatus() != null && dictType.getStatus() == 1 ? "启用" : "禁用");
        dto.setSortOrder(dictType.getSortOrder());
        dto.setRemark(dictType.getRemark());
        dto.setCreateTime(dictType.getCreateTime());
        dto.setUpdateTime(dictType.getUpdateTime());
        return dto;
    }

    /**
     * 转换字典项为DTO
     */
    private DictItemDTO convertToDictItemDTO(DictItem dictItem) {
        DictItemDTO dto = new DictItemDTO();
        dto.setId(dictItem.getId());
        dto.setDictTypeId(dictItem.getDictTypeId());
        
        // 获取字典类型信息
        DictType dictType = dictTypeMapper.selectById(dictItem.getDictTypeId());
        if (dictType != null) {
            dto.setDictCode(dictType.getDictCode());
            dto.setDictTypeName(dictType.getDictTypeName());
        }
        
        dto.setDictItemCode(dictItem.getDictItemCode());
        dto.setDictItemName(dictItem.getDictItemName());
        dto.setDictItemValue(dictItem.getDictItemValue());
        dto.setDictItemLabel(dictItem.getDictItemLabel());
        dto.setStatus(dictItem.getStatus());
        dto.setStatusDesc(dictItem.getStatus() != null && dictItem.getStatus() == 1 ? "启用" : "禁用");
        dto.setSortOrder(dictItem.getSortOrder());
        dto.setColorStyle(dictItem.getColorStyle());
        dto.setIcon(dictItem.getIcon());
        dto.setRemark(dictItem.getRemark());
        dto.setCreateTime(dictItem.getCreateTime());
        dto.setUpdateTime(dictItem.getUpdateTime());
        return dto;
    }
}