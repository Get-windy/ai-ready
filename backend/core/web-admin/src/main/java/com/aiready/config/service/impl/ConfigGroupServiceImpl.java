package com.aiready.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.aiready.config.dto.ConfigGroupDTO;
import com.aiready.config.dto.ConfigGroupRequest;
import com.aiready.config.entity.ConfigGroup;
import com.aiready.config.mapper.ConfigGroupMapper;
import com.aiready.config.service.ConfigGroupService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 配置组服务实现
 */
@Service
@RequiredArgsConstructor
public class ConfigGroupServiceImpl extends ServiceImpl<ConfigGroupMapper, ConfigGroup> 
        implements ConfigGroupService {
    
    private final ConfigGroupMapper groupMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfigGroupDTO createGroup(ConfigGroupRequest request, Long operatorId) {
        // 检查组编码
        if (groupMapper.existsByCode(request.getGroupCode()) > 0) {
            throw new RuntimeException("组编码已存在: " + request.getGroupCode());
        }
        
        ConfigGroup group = new ConfigGroup();
        BeanUtil.copyProperties(request, group);
        
        // 计算层级
        if (request.getParentId() != null) {
            ConfigGroup parent = groupMapper.selectById(request.getParentId());
            if (parent != null) {
                group.setPath(parent.getPath() + "/" + request.getGroupCode());
                group.setDepth(parent.getDepth() + 1);
            }
        } else {
            group.setPath("/" + request.getGroupCode());
            group.setDepth(0);
        }
        
        group.setCreateBy(operatorId);
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateBy(operatorId);
        group.setUpdateTime(LocalDateTime.now());
        
        groupMapper.insert(group);
        
        return convertToDTO(group);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfigGroupDTO updateGroup(Long groupId, ConfigGroupRequest request, Long operatorId) {
        ConfigGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("配置组不存在");
        }
        
        // 系统内置组不能修改编码
        if (group.getBuiltIn() != null && group.getBuiltIn() == 1) {
            if (!group.getGroupCode().equals(request.getGroupCode())) {
                throw new RuntimeException("系统内置组不能修改编码");
            }
        }
        
        BeanUtil.copyProperties(request, group);
        group.setId(groupId);
        group.setUpdateBy(operatorId);
        group.setUpdateTime(LocalDateTime.now());
        
        groupMapper.updateById(group);
        
        return convertToDTO(group);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long groupId, Long operatorId) {
        ConfigGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            return;
        }
        
        if (group.getBuiltIn() != null && group.getBuiltIn() == 1) {
            throw new RuntimeException("系统内置组不能删除");
        }
        
        // 检查是否有子组
        List<ConfigGroup> children = groupMapper.selectByParentId(groupId);
        if (!children.isEmpty()) {
            throw new RuntimeException("该组下存在子组，不能删除");
        }
        
        // 检查是否有配置项
        Integer configCount = groupMapper.countConfigs(groupId);
        if (configCount > 0) {
            throw new RuntimeException("该组下存在配置项，不能删除");
        }
        
        groupMapper.deleteById(groupId);
    }
    
    @Override
    public ConfigGroupDTO getGroupById(Long groupId) {
        ConfigGroup group = groupMapper.selectById(groupId);
        return group != null ? convertToDTO(group) : null;
    }
    
    @Override
    public ConfigGroupDTO getGroupByCode(String groupCode) {
        ConfigGroup group = groupMapper.selectByCode(groupCode);
        return group != null ? convertToDTO(group) : null;
    }
    
    @Override
    public List<ConfigGroupDTO> getAllGroups() {
        List<ConfigGroup> groups = groupMapper.selectAllActive();
        return buildGroupTree(groups);
    }
    
    @Override
    public List<ConfigGroupDTO> getChildGroups(Long parentId) {
        List<ConfigGroup> groups = groupMapper.selectByParentId(parentId);
        return groups.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfigGroupDTO moveGroup(Long groupId, Long newParentId, Long operatorId) {
        ConfigGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new RuntimeException("配置组不存在");
        }
        
        group.setParentId(newParentId);
        
        // 重新计算层级
        if (newParentId != null) {
            ConfigGroup parent = groupMapper.selectById(newParentId);
            if (parent != null) {
                group.setPath(parent.getPath() + "/" + group.getGroupCode());
                group.setDepth(parent.getDepth() + 1);
            }
        } else {
            group.setPath("/" + group.getGroupCode());
            group.setDepth(0);
        }
        
        group.setUpdateBy(operatorId);
        group.setUpdateTime(LocalDateTime.now());
        
        groupMapper.updateById(group);
        
        return convertToDTO(group);
    }
    
    private List<ConfigGroupDTO> buildGroupTree(List<ConfigGroup> groups) {
        Map<Long, ConfigGroupDTO> dtoMap = groups.stream()
                .collect(Collectors.toMap(ConfigGroup::getId, this::convertToDTO));
        
        List<ConfigGroupDTO> tree = new ArrayList<>();
        
        for (ConfigGroupDTO dto : dtoMap.values()) {
            if (dto.getParentId() == null) {
                tree.add(dto);
            } else {
                ConfigGroupDTO parent = dtoMap.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                }
            }
        }
        
        return tree;
    }
    
    private ConfigGroupDTO convertToDTO(ConfigGroup group) {
        ConfigGroupDTO dto = new ConfigGroupDTO();
        BeanUtil.copyProperties(group, dto);
        
        // 统计配置数量
        Integer configCount = groupMapper.countConfigs(group.getId());
        dto.setConfigCount(configCount);
        
        return dto;
    }
}
