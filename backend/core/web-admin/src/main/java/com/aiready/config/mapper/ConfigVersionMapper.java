package com.aiready.config.mapper;

import com.aiready.config.entity.ConfigVersion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配置版本Mapper
 */
@Mapper
public interface ConfigVersionMapper extends BaseMapper<ConfigVersion> {
    
    /**
     * 查询配置的所有版本
     */
    @Select("SELECT * FROM sys_config_version WHERE config_id = #{configId} ORDER BY version DESC")
    List<ConfigVersion> selectByConfigId(@Param("configId") Long configId);
    
    /**
     * 查询配置的最新版本
     */
    @Select("SELECT * FROM sys_config_version WHERE config_id = #{configId} ORDER BY version DESC LIMIT 1")
    ConfigVersion selectLatestVersion(@Param("configId") Long configId);
    
    /**
     * 获取最大版本号
     */
    @Select("SELECT COALESCE(MAX(version), 0) FROM sys_config_version WHERE config_id = #{configId}")
    Integer getMaxVersion(@Param("configId") Long configId);
    
    /**
     * 查询指定版本
     */
    @Select("SELECT * FROM sys_config_version WHERE config_id = #{configId} AND version = #{version}")
    ConfigVersion selectByVersion(@Param("configId") Long configId, @Param("version") Integer version);
}
