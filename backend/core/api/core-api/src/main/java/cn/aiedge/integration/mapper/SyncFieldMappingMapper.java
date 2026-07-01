package cn.aiedge.integration.mapper;

import cn.aiedge.integration.model.SyncFieldMapping;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 同步字段映射 Mapper
 */
@Mapper
public interface SyncFieldMappingMapper extends BaseMapper<SyncFieldMapping> {

    /**
     * 根据配置ID和单据类型查询启用的字段映射
     */
    @Select("SELECT * FROM sync_field_mapping WHERE source_config_id = #{configId} AND bill_type = #{billType} AND status = 1 AND deleted = 0 ORDER BY sort_order ASC")
    List<SyncFieldMapping> selectByConfigAndBillType(@Param("configId") Long configId, @Param("billType") String billType);

    /**
     * 根据配置ID查询所有字段映射
     */
    @Select("SELECT * FROM sync_field_mapping WHERE source_config_id = #{configId} AND deleted = 0 ORDER BY bill_type, sort_order ASC")
    List<SyncFieldMapping> selectByConfigId(@Param("configId") Long configId);

    /**
     * 根据配置ID删除所有字段映射（批量清理）
     */
    @Delete("DELETE FROM sync_field_mapping WHERE source_config_id = #{configId}")
    int deleteByConfigId(@Param("configId") Long configId);
}
