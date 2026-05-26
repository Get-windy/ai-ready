package cn.aiedge.dict.service.impl;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.common.utils.BeanCopyUtils;
import cn.aiedge.dict.dto.DictItemDTO;
import cn.aiedge.dict.mapper.DictItemMapper;
import cn.aiedge.dict.mapper.DictTypeMapper;
import cn.aiedge.dict.model.DictItem;
import cn.aiedge.dict.model.DictType;
import cn.aiedge.dict.service.DictItemService;
import cn.aiedge.dict.vo.DictItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典项服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictItemServiceImpl implements DictItemService {

    private static final String CACHE_KEY_PREFIX = "dict:item:";
    private static final String CACHE_KEY_TYPE = "dict:items:";

    private final DictItemMapper dictItemMapper;
    private final DictTypeMapper dictTypeMapper;
    private final CacheService cacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DictItemDTO dictItemDTO) {
        // 检查字典类型是否存在
        DictType dictType = dictTypeMapper.selectById(dictItemDTO.getDictTypeId());
        if (dictType == null) {
            throw new RuntimeException("字典类型不存在");
        }
        
        // 检查同一字典类型下项值的唯一性
        Long tenantId = getCurrentTenantId();
        DictItem exist = dictItemMapper.selectByDictCodeAndValue(dictType.getDictCode(), 
            dictItemDTO.getItemValue(), tenantId);
        if (exist != null) {
            throw new RuntimeException("字典项值已存在: " + dictItemDTO.getItemValue());
        }
        
        DictItem dictItem = new DictItem();
        BeanCopyUtils.copyProperties(dictItemDTO, dictItem);
        if (dictItem.getParentId() == null) {
            dictItem.setParentId(0L);
        }
        if (dictItem.getSortOrder() == null) {
            dictItem.setSortOrder(0);
        }
        if (dictItem.getStatus() == null) {
            dictItem.setStatus(DictItem.Status.ENABLED.getCode());
        }
        if (dictItem.getIsDefault() == null) {
            dictItem.setIsDefault("N");
        }
        
        dictItemMapper.insert(dictItem);
        
        // 刷新缓存
        refreshCache(dictType.getDictCode());
        
        return dictItem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreate(List<DictItemDTO> dictItemDTOs) {
        if (dictItemDTOs == null || dictItemDTOs.isEmpty()) {
            return 0;
        }
        
        int count = 0;
        String dictCode = null;
        
        for (DictItemDTO dto : dictItemDTOs) {
            try {
                Long id = create(dto);
                if (id != null) {
                    count++;
                    if (dictCode == null) {
                        DictType dictType = dictTypeMapper.selectById(dto.getDictTypeId());
                        if (dictType != null) {
                            dictCode = dictType.getDictCode();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("批量创建字典项失败: {}", e.getMessage());
            }
        }
        
        // 刷新缓存
        if (dictCode != null) {
            refreshCache(dictCode);
        }
        
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(DictItemDTO dictItemDTO) {
        DictItem exist = dictItemMapper.selectById(dictItemDTO.getId());
        if (exist == null) {
            throw new RuntimeException("字典项不存在");
        }
        
        // 检查同一字典类型下项值的唯一性（排除自己）
        if (!exist.getItemValue().equals(dictItemDTO.getItemValue())) {
            DictType dictType = dictTypeMapper.selectById(exist.getDictTypeId());
            Long tenantId = getCurrentTenantId();
            DictItem same = dictItemMapper.selectByDictCodeAndValue(dictType.getDictCode(), 
                dictItemDTO.getItemValue(), tenantId);
            if (same != null) {
                throw new RuntimeException("字典项值已存在: " + dictItemDTO.getItemValue());
            }
        }
        
        BeanCopyUtils.copyProperties(dictItemDTO, exist);
        if (dictItemDTO.getParentId() == null) {
            exist.setParentId(0L);
        }
        dictItemMapper.updateById(exist);
        
        // 刷新缓存
        DictType dictType = dictTypeMapper.selectById(exist.getDictTypeId());
        if (dictType != null) {
            refreshCache(dictType.getDictCode());
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        DictItem exist = dictItemMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("字典项不存在");
        }
        
        // 检查是否有子字典项
        long childCount = dictItemMapper.selectCount(
            new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getParentId, id)
                .eq(DictItem::getDeleted, 0)
        );
        if (childCount > 0) {
            throw new RuntimeException("存在子字典项，无法删除");
        }
        
        dictItemMapper.deleteById(id);
        
        // 刷新缓存
        DictType dictType = dictTypeMapper.selectById(exist.getDictTypeId());
        if (dictType != null) {
            refreshCache(dictType.getDictCode());
        }
        
        return true;
    }

    @Override
    public DictItemVO getById(Long id) {
        DictItem dictItem = dictItemMapper.selectById(id);
        if (dictItem == null) {
            return null;
        }
        return convertToVO(dictItem);
    }

    @Override
    public List<DictItemVO> getByDictTypeId(Long dictTypeId) {
        // 先从缓存获取
        String cacheKey = CACHE_KEY_TYPE + dictTypeId;
        List<DictItemVO> cached = cacheService.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }
        
        List<DictItem> list = dictItemMapper.selectEnabledByType(dictTypeId);
        List<DictItemVO> result = list.stream().map(this::convertToVO).collect(Collectors.toList());
        
        // 缓存结果
        cacheService.set(cacheKey, result);
        
        return result;
    }

    @Override
    public List<DictItemVO> getByDictCode(String dictCode) {
        // 先从缓存获取
        String cacheKey = CACHE_KEY_PREFIX + dictCode;
        List<DictItemVO> cached = cacheService.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }
        
        Long tenantId = getCurrentTenantId();
        DictType dictType = dictTypeMapper.selectByDictCode(dictCode, tenantId);
        if (dictType == null) {
            return Collections.emptyList();
        }
        
        List<DictItem> list = dictItemMapper.selectEnabledByType(dictType.getId());
        List<DictItemVO> result = list.stream().map(this::convertToVO).collect(Collectors.toList());
        
        // 缓存结果
        cacheService.set(cacheKey, result);
        
        return result;
    }

    @Override
    public List<DictItemVO> getTree(Long dictTypeId, Long parentId) {
        if (parentId == null) {
            parentId = 0L;
        }
        
        List<DictItem> list = dictItemMapper.selectTree(dictTypeId, parentId);
        return buildTree(list, parentId);
    }

    @Override
    public Map<String, Object> list(Map<String, Object> params) {
        Long tenantId = params.get("tenantId") != null ? Long.parseLong(params.get("tenantId").toString()) : getCurrentTenantId();
        Long dictTypeId = params.get("dictTypeId") != null ? Long.parseLong(params.get("dictTypeId").toString()) : null;
        String itemValue = (String) params.get("itemValue");
        String itemText = (String) params.get("itemText");
        String status = (String) params.get("status");
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int pageSize = params.get("pageSize") != null ? Integer.parseInt(params.get("pageSize").toString()) : 10;

        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<DictItem>()
            .eq(DictItem::getDeleted, 0)
            .eq(tenantId != null, DictItem::getTenantId, tenantId)
            .eq(dictTypeId != null, DictItem::getDictTypeId, dictTypeId)
            .like(StringUtils.hasText(itemValue), DictItem::getItemValue, itemValue)
            .like(StringUtils.hasText(itemText), DictItem::getItemText, itemText)
            .eq(StringUtils.hasText(status), DictItem::getStatus, status)
            .orderByAsc(DictItem::getSortOrder)
            .orderByDesc(DictItem::getCreateTime);

        IPage<DictItem> pageResult = dictItemMapper.selectPage(new Page<>(page, pageSize), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, String status) {
        DictItem exist = dictItemMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("字典项不存在");
        }
        
        exist.setStatus(status);
        dictItemMapper.updateById(exist);
        
        // 刷新缓存
        DictType dictType = dictTypeMapper.selectById(exist.getDictTypeId());
        if (dictType != null) {
            refreshCache(dictType.getDictCode());
        }
        
        return true;
    }

    @Override
    public DictItemVO getByDictCodeAndValue(String dictCode, String itemValue) {
        Long tenantId = getCurrentTenantId();
        DictItem dictItem = dictItemMapper.selectByDictCodeAndValue(dictCode, itemValue, tenantId);
        if (dictItem == null) {
            return null;
        }
        return convertToVO(dictItem);
    }

    @Override
    public List<DictItem> export(Map<String, Object> params) {
        Long tenantId = params.get("tenantId") != null ? Long.parseLong(params.get("tenantId").toString()) : getCurrentTenantId();
        Long dictTypeId = params.get("dictTypeId") != null ? Long.parseLong(params.get("dictTypeId").toString()) : null;
        String itemValue = (String) params.get("itemValue");
        String itemText = (String) params.get("itemText");
        String status = (String) params.get("status");

        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<DictItem>()
            .eq(DictItem::getDeleted, 0)
            .eq(tenantId != null, DictItem::getTenantId, tenantId)
            .eq(dictTypeId != null, DictItem::getDictTypeId, dictTypeId)
            .like(StringUtils.hasText(itemValue), DictItem::getItemValue, itemValue)
            .like(StringUtils.hasText(itemText), DictItem::getItemText, itemText)
            .eq(StringUtils.hasText(status), DictItem::getStatus, status)
            .orderByAsc(DictItem::getSortOrder);

        return dictItemMapper.selectList(wrapper);
    }

    @Override
    public void refreshCache(String dictCode) {
        Long tenantId = getCurrentTenantId();
        DictType dictType = dictTypeMapper.selectByDictCode(dictCode, tenantId);
        if (dictType == null) {
            return;
        }
        
        // 清除旧缓存
        cacheService.evict(CACHE_KEY_PREFIX + dictCode);
        cacheService.evict(CACHE_KEY_TYPE + dictType.getId());
        
        // 重新查询并缓存
        List<DictItem> list = dictItemMapper.selectEnabledByType(dictType.getId());
        List<DictItemVO> result = list.stream().map(this::convertToVO).collect(Collectors.toList());
        
        cacheService.set(CACHE_KEY_PREFIX + dictCode, result);
        cacheService.set(CACHE_KEY_TYPE + dictType.getId(), result);
    }

    @Override
    public int countByDictType(Long dictTypeId) {
        return dictItemMapper.countByDictType(dictTypeId);
    }

    private List<DictItemVO> buildTree(List<DictItem> list, Long parentId) {
        return list.stream()
            .filter(item -> item.getParentId().equals(parentId))
            .map(item -> {
                DictItemVO vo = convertToVO(item);
                List<DictItemVO> children = buildTree(list, item.getId());
                if (!children.isEmpty()) {
                    vo.setChildren(children);
                }
                return vo;
            })
            .collect(Collectors.toList());
    }

    private DictItemVO convertToVO(DictItem dictItem) {
        DictItemVO vo = new DictItemVO();
        BeanCopyUtils.copyProperties(dictItem, vo);
        return vo;
    }

    private Long getCurrentTenantId() {
        // 从上下文获取租户ID，这里简单返回1L
        return 1L;
    }
}
