package cn.aiedge.dict.service.impl;

import cn.aiedge.cache.service.CacheService;
import cn.aiedge.common.utils.BeanCopyUtils;
import cn.aiedge.dict.dto.DictTypeDTO;
import cn.aiedge.dict.mapper.DictTypeMapper;
import cn.aiedge.dict.model.DictType;
import cn.aiedge.dict.service.DictItemService;
import cn.aiedge.dict.service.DictTypeService;
import cn.aiedge.dict.vo.DictTypeVO;
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
 * 字典类型服务实现类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictTypeServiceImpl implements DictTypeService {

    private static final String CACHE_KEY_PREFIX = "dict:type:";
    private static final String CACHE_KEY_ALL = "dict:all:types";

    private final DictTypeMapper dictTypeMapper;
    private final DictItemService dictItemService;
    private final CacheService cacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DictTypeDTO dictTypeDTO) {
        // 检查编码唯一性
        DictType exist = dictTypeMapper.selectByDictCode(dictTypeDTO.getDictCode(), dictTypeDTO.getTenantId());
        if (exist != null) {
            throw new RuntimeException("字典类型编码已存在: " + dictTypeDTO.getDictCode());
        }

        DictType dictType = new DictType();
        BeanCopyUtils.copyProperties(dictTypeDTO, dictType);
        if (dictType.getParentId() == null) {
            dictType.setParentId(0L);
        }
        if (dictType.getSortOrder() == null) {
            dictType.setSortOrder(0);
        }
        if (dictType.getStatus() == null) {
            dictType.setStatus(DictType.Status.ENABLED.getCode());
        }
        if (dictType.getIsBuiltIn() == null) {
            dictType.setIsBuiltIn("N");
        }

        dictTypeMapper.insert(dictType);
        
        // 清除缓存
        clearAllCache();
        
        return dictType.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(DictTypeDTO dictTypeDTO) {
        DictType exist = dictTypeMapper.selectById(dictTypeDTO.getId());
        if (exist == null) {
            throw new RuntimeException("字典类型不存在");
        }
        
        // 内置字典不能修改关键信息
        if ("Y".equals(exist.getIsBuiltIn())) {
            // 只允许修改描述、排序、状态
            exist.setDescription(dictTypeDTO.getDescription());
            exist.setSortOrder(dictTypeDTO.getSortOrder());
            exist.setStatus(dictTypeDTO.getStatus());
            exist.setRemark(dictTypeDTO.getRemark());
            dictTypeMapper.updateById(exist);
        } else {
            // 检查编码唯一性
            if (!exist.getDictCode().equals(dictTypeDTO.getDictCode())) {
                DictType sameCode = dictTypeMapper.selectByDictCode(dictTypeDTO.getDictCode(), dictTypeDTO.getTenantId());
                if (sameCode != null) {
                    throw new RuntimeException("字典类型编码已存在: " + dictTypeDTO.getDictCode());
                }
            }
            
            BeanCopyUtils.copyProperties(dictTypeDTO, exist);
            if (dictTypeDTO.getParentId() == null) {
                exist.setParentId(0L);
            }
            dictTypeMapper.updateById(exist);
        }

        // 清除缓存
        clearCache(exist.getDictCode());
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        DictType exist = dictTypeMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("字典类型不存在");
        }
        
        // 内置字典不能删除
        if ("Y".equals(exist.getIsBuiltIn())) {
            throw new RuntimeException("内置字典类型不能删除");
        }
        
        // 检查是否有子字典类型
        long childCount = dictTypeMapper.selectCount(
            new LambdaQueryWrapper<DictType>()
                .eq(DictType::getParentId, id)
                .eq(DictType::getDeleted, 0)
        );
        if (childCount > 0) {
            throw new RuntimeException("存在子字典类型，无法删除");
        }
        
        // 检查是否有字典项
        int itemCount = dictItemService.countByDictType(id);
        if (itemCount > 0) {
            throw new RuntimeException("存在字典项，无法删除");
        }
        
        dictTypeMapper.deleteById(id);
        
        // 清除缓存
        clearCache(exist.getDictCode());
        
        return true;
    }

    @Override
    public DictTypeVO getById(Long id) {
        // 先从缓存获取
        String cacheKey = CACHE_KEY_PREFIX + id;
        DictTypeVO cached = cacheService.get(cacheKey, DictTypeVO.class);
        if (cached != null) {
            return cached;
        }
        
        DictType dictType = dictTypeMapper.selectById(id);
        if (dictType == null) {
            return null;
        }
        
        DictTypeVO vo = convertToVO(dictType);
        
        // 缓存结果
        cacheService.set(cacheKey, vo);
        
        return vo;
    }

    @Override
    public DictTypeVO getByDictCode(String dictCode) {
        // 先从缓存获取
        String cacheKey = CACHE_KEY_PREFIX + "code:" + dictCode;
        DictTypeVO cached = cacheService.get(cacheKey, DictTypeVO.class);
        if (cached != null) {
            return cached;
        }
        
        Long tenantId = getCurrentTenantId();
        DictType dictType = dictTypeMapper.selectByDictCode(dictCode, tenantId);
        if (dictType == null) {
            return null;
        }
        
        DictTypeVO vo = convertToVO(dictType);
        
        // 缓存结果
        cacheService.set(cacheKey, vo);
        
        return vo;
    }

    @Override
    public Map<String, Object> list(Map<String, Object> params) {
        Long tenantId = params.get("tenantId") != null ? Long.parseLong(params.get("tenantId").toString()) : getCurrentTenantId();
        String dictCode = (String) params.get("dictCode");
        String dictName = (String) params.get("dictName");
        String status = (String) params.get("status");
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int pageSize = params.get("pageSize") != null ? Integer.parseInt(params.get("pageSize").toString()) : 10;

        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<DictType>()
            .eq(DictType::getDeleted, 0)
            .eq(tenantId != null, DictType::getTenantId, tenantId)
            .like(StringUtils.hasText(dictCode), DictType::getDictCode, dictCode)
            .like(StringUtils.hasText(dictName), DictType::getDictName, dictName)
            .eq(StringUtils.hasText(status), DictType::getStatus, status)
            .orderByAsc(DictType::getSortOrder)
            .orderByDesc(DictType::getCreateTime);

        IPage<DictType> pageResult = dictTypeMapper.selectPage(new Page<>(page, pageSize), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
        result.put("total", pageResult.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return result;
    }

    @Override
    public List<DictTypeVO> getTree(Long parentId) {
        Long tenantId = getCurrentTenantId();
        if (parentId == null) {
            parentId = 0L;
        }
        
        List<DictType> list = dictTypeMapper.selectTree(tenantId, parentId);
        return buildTree(list, parentId);
    }

    @Override
    public List<DictTypeVO> getEnabled() {
        // 先从缓存获取
        List<DictTypeVO> cached = cacheService.get(CACHE_KEY_ALL, List.class);
        if (cached != null) {
            return cached;
        }
        
        Long tenantId = getCurrentTenantId();
        List<DictType> list = dictTypeMapper.selectEnabled(tenantId);
        List<DictTypeVO> result = list.stream().map(this::convertToVO).collect(Collectors.toList());
        
        // 缓存结果
        cacheService.set(CACHE_KEY_ALL, result);
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, String status) {
        DictType exist = dictTypeMapper.selectById(id);
        if (exist == null) {
            throw new RuntimeException("字典类型不存在");
        }
        
        exist.setStatus(status);
        dictTypeMapper.updateById(exist);
        
        // 清除缓存
        clearCache(exist.getDictCode());
        
        return true;
    }

    @Override
    public List<DictType> export(Map<String, Object> params) {
        Long tenantId = params.get("tenantId") != null ? Long.parseLong(params.get("tenantId").toString()) : getCurrentTenantId();
        String dictCode = (String) params.get("dictCode");
        String dictName = (String) params.get("dictName");
        String status = (String) params.get("status");

        LambdaQueryWrapper<DictType> wrapper = new LambdaQueryWrapper<DictType>()
            .eq(DictType::getDeleted, 0)
            .eq(tenantId != null, DictType::getTenantId, tenantId)
            .like(StringUtils.hasText(dictCode), DictType::getDictCode, dictCode)
            .like(StringUtils.hasText(dictName), DictType::getDictName, dictName)
            .eq(StringUtils.hasText(status), DictType::getStatus, status)
            .orderByAsc(DictType::getSortOrder);

        return dictTypeMapper.selectList(wrapper);
    }

    @Override
    public void clearCache(String dictCode) {
        cacheService.evict(CACHE_KEY_PREFIX + dictCode);
        cacheService.evict(CACHE_KEY_PREFIX + "code:" + dictCode);
        cacheService.evict(CACHE_KEY_ALL);
    }

    @Override
    public void clearAllCache() {
        // 清除所有字典缓存需要遍历或使用模式匹配
        // 这里简单清除公共缓存key
        cacheService.evict(CACHE_KEY_ALL);
    }

    private List<DictTypeVO> buildTree(List<DictType> list, Long parentId) {
        return list.stream()
            .filter(item -> item.getParentId().equals(parentId))
            .map(item -> {
                DictTypeVO vo = convertToVO(item);
                List<DictTypeVO> children = buildTree(list, item.getId());
                if (!children.isEmpty()) {
                    vo.setChildren(children);
                }
                return vo;
            })
            .collect(Collectors.toList());
    }

    private DictTypeVO convertToVO(DictType dictType) {
        DictTypeVO vo = new DictTypeVO();
        BeanCopyUtils.copyProperties(dictType, vo);
        return vo;
    }

    private Long getCurrentTenantId() {
        // 从上下文获取租户ID，这里简单返回1L
        // 实际应从SecurityContext或ThreadLocal获取
        return 1L;
    }
}
