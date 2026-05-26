package com.aiready.config.service;

import com.aiready.config.dto.ConfigGroupDTO;
import com.aiready.config.dto.ConfigGroupRequest;
import com.aiready.config.entity.ConfigGroup;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 配置组服务接口
 */
public interface ConfigGroupService extends IService<ConfigGroup> {
    
    /**
     * 创建配置组
     */
    ConfigGroupDTO createGroup(ConfigGroupRequest request, Long operatorId);
    
    /**
     * 更新配置组
     */
    ConfigGroupDTO updateGroup(Long groupId, ConfigGroupRequest request, Long operatorId);
    
    /**
     * 删除配置组
     */
    void deleteGroup(Long groupId, Long operatorId);
    
    /**
     * 获取配置组详情
     */
    ConfigGroupDTO getGroupById(Long groupId);
    
    /**
     * 获取配置组详情（根据编码）
     */
    ConfigGroupDTO getGroupByCode(String groupCode);
    
    /**
     * 获取所有配置组（树形结构）
     */
    List<ConfigGroupDTO> getAllGroups();
    
    /**
     * 获取子组
     */
    List<ConfigGroupDTO> getChildGroups(Long parentId);
    
    /**
     * 移动配置组
     */
    ConfigGroupDTO moveGroup(Long groupId, Long newParentId, Long operatorId);
}
