package com.aiready.dict.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.aiready.dict.dto.*;
import com.aiready.dict.entity.DictType;
import com.aiready.dict.mapper.DictTypeMapper;
import com.aiready.dict.service.DictCacheService;
import com.aiready.dict.service.DictTypeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典类型服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictTypeServiceImpl extends ServiceImpl<DictTypeMapper, DictType> 
        implements DictTypeService {
    
    private final DictTypeMapper dictTypeMapper;
    private final DictCacheService dictCacheService;
    
    @Override
    public DictTypeDTO getByDictCode(String dictCode) {
        DictType dictType = dictTypeMapper.selectByDictCode(dictCode);
        return dictType != null ? convertToDTO(dictType) : null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictTypeDTO saveDictType(DictTypeSaveRequest request, Long operatorId) {
        // 检查字典编码是否已存在
        if (existsByDictCode(request.getDictCode())) {
            throw new RuntimeException("字典编码已存在: " + request.getDictCode());
        }
        
        DictType dictType = new DictType();
        BeanUtil.copyProperties(request, dictType);
        
        if (dictType.getStatus() == null) {
            dictType.setStatus(1);
        }
        if (dictType.getIsSystem() == null) {
            dictType.setIsSystem(0);
        }
        if (dictType.getSortOrder() == null) {
            dictType.setSortOrder(dictTypeMapper.getMaxSortOrder() + 1);
        }
        
        dictType.setCreateBy(operatorId);
        dictType.setCreateTime(LocalDateTime.now());
        dictType.setUpdateBy(operatorId);
        dictType.setUpdateTime(LocalDateTime.now());
        dictType.setDeleted(0);
        
        dictTypeMapper.insert(dictType);
        
        log.info("创建字典类型: {}", dictType.getDictCode());
        
        return convertToDTO(dictType);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictTypeDTO updateDictType(Long dictTypeId, DictTypeSaveRequest request, Long operatorId) {
        DictType dictType = dictTypeMapper.selectById(dictTypeId);
        if (dictType == null) {
            throw new RuntimeException("字典类型不存在");
        }
        
        // 检查是否系统内置
        if (dictType.getIsSystem() != null && dictType.getIsSystem() == 1) {
            throw new RuntimeException("系统内置字典不允许修改");
        }
        
        // 检查字典编码是否重复
        if (!dictType.getDictCode().equals(request.getDictCode()) 
                && existsByDictCode(request.getDictCode())) {
            throw new RuntimeException("字典编码已存在: " + request.getDictCode());
        }
        
        String oldDictCode = dictType.getDictCode();
        
        BeanUtil.copyProperties(request, dictType);
        dictType.setId(dictTypeId);
        dictType.setUpdateBy(operatorId);
        dictType.setUpdateTime(LocalDateTime.now());
        
        dictTypeMapper.updateById(dictType);
        
        // 清除缓存
        dictCacheService.evictDictCache(oldDictCode);
        if (!oldDictCode.equals(request.getDictCode())) {
            dictCacheService.evictDictCache(request.getDictCode());
        }
        
        log.info("更新字典类型: {}", dictType.getDictCode());
        
        return convertToDTO(dictType);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictType(Long dictTypeId, Long operatorId) {
        DictType dictType = dictTypeMapper.selectById(dictTypeId);
        if (dictType == null) {
            return;
        }
        
        // 检查是否系统内置
        if (dictType.getIsSystem() != null && dictType.getIsSystem() == 1) {
            throw new RuntimeException("系统内置字典不允许删除");
        }
        
        // 检查是否有字典项
        Integer itemCount = dictTypeMapper.countItemsByDictTypeId(dictTypeId);
        if (itemCount != null && itemCount > 0) {
            throw new RuntimeException("字典类型下存在字典项，请先删除字典项");
        }
        
        dictTypeMapper.deleteById(dictTypeId);
        
        // 清除缓存
        dictCacheService.evictDictCache(dictType.getDictCode());
        
        log.info("删除字典类型: {}", dictType.getDictCode());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDictTypes(List<Long> dictTypeIds, Long operatorId) {
        if (dictTypeIds != null) {
            for (Long dictTypeId : dictTypeIds) {
                deleteDictType(dictTypeId, operatorId);
            }
        }
    }
    
    @Override
    public IPage<DictTypeDTO> queryDictTypes(DictQueryRequest request) {
        Page<DictType> pageParam = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<>();
        
        if (request.getDictCode() != null && !request.getDictCode().isEmpty()) {
            wrapper.like(DictType::getDictCode, request.getDictCode());
        }
        if (request.getDictName() != null && !request.getDictName().isEmpty()) {
            wrapper.like(DictType::getDictName, request.getDictName());
        }
        if (request.getStatus() != null) {
            wrapper.eq(DictType::getStatus, request.getStatus());
        }
        if (request.getIsSystem() != null) {
            wrapper.eq(DictType::getIsSystem, request.getIsSystem());
        }
        if (request.getCreateTimeStart() != null) {
            wrapper.ge(DictType::getCreateTime, request.getCreateTimeStart());
        }
        if (request.getCreateTimeEnd() != null) {
            wrapper.le(DictType::getCreateTime, request.getCreateTimeEnd());
        }
        
        wrapper.eq(DictType::getDeleted, 0);
        wrapper.orderByAsc(DictType::getSortOrder).orderByDesc(DictType::getId);
        
        IPage<DictType> dictTypePage = dictTypeMapper.selectPage(pageParam, wrapper);
        return dictTypePage.convert(this::convertToDTO);
    }
    
    @Override
    public List<DictTypeDTO> getAllActiveDictTypes() {
        List<DictType> dictTypes = dictTypeMapper.selectAllActive();
        return dictTypes.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictTypeDTO updateStatus(Long dictTypeId, Integer status, Long operatorId) {
        DictType dictType = dictTypeMapper.selectById(dictTypeId);
        if (dictType == null) {
            throw new RuntimeException("字典类型不存在");
        }
        
        dictTypeMapper.updateStatus(dictTypeId, status);
        
        // 清除缓存
        dictCacheService.evictDictCache(dictType.getDictCode());
        
        dictType.setStatus(status);
        log.info("更新字典类型状态: {} -> {}", dictType.getDictCode(), status);
        
        return convertToDTO(dictType);
    }
    
    @Override
    public boolean existsByDictCode(String dictCode) {
        Integer count = dictTypeMapper.existsByDictCode(dictCode);
        return count != null && count > 0;
    }
    
    @Override
    public boolean existsByDictCodeExcludeId(String dictCode, Long excludeId) {
        Integer count = dictTypeMapper.existsByDictCodeExcludeId(dictCode, excludeId);
        return count != null && count > 0;
    }
    
    private DictTypeDTO convertToDTO(DictType dictType) {
        DictTypeDTO dto = new DictTypeDTO();
        BeanUtil.copyProperties(dictType, dto);
        
        // 状态名称
        dto.setStatusName(dictType.getStatus() != null && dictType.getStatus() == 1 ? "启用" : "禁用");
        
        // 字典项数量
        dto.setItemCount(dictTypeMapper.countItemsByDictTypeId(dictType.getId()));
        
        return dto;
    }
}
