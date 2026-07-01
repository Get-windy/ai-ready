package cn.aiedge.integration.service;

import cn.aiedge.integration.model.SyncFieldMapping;

import java.util.List;

/**
 * 同步字段映射服务接口
 */
public interface SyncFieldMappingService {

    /**
     * 查询指定配置下的所有字段映射
     */
    List<SyncFieldMapping> listByConfigId(Long configId);

    /**
     * 查询指定配置+单据类型的字段映射
     */
    List<SyncFieldMapping> listByConfigAndBillType(Long configId, String billType);

    /**
     * 批量保存字段映射（先删后插）
     */
    List<SyncFieldMapping> batchSave(Long configId, List<SyncFieldMapping> mappings);

    /**
     * 创建单条字段映射
     */
    SyncFieldMapping create(SyncFieldMapping mapping);

    /**
     * 更新字段映射
     */
    SyncFieldMapping update(Long id, SyncFieldMapping mapping);

    /**
     * 删除字段映射
     */
    boolean delete(Long id);

    /**
     * 复制字段映射模板（从预置模板初始化）
     */
    List<SyncFieldMapping> initFromTemplate(Long configId, String billType);
}
