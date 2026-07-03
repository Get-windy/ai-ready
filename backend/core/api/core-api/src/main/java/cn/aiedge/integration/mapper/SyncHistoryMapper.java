package cn.aiedge.integration.mapper;

import cn.aiedge.integration.model.SyncHistory;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 同步历史 Mapper
 */
@Mapper
public interface SyncHistoryMapper extends BaseMapper<SyncHistory> {

    /**
     * 根据配置ID查询同步历史（最近50条）
     */
    @Select("SELECT * FROM sync_history WHERE config_id = #{configId} ORDER BY start_time DESC LIMIT 50")
    List<SyncHistory> selectByConfigId(@Param("configId") Long configId);

    /**
     * 查询所有同步历史（最近100条）
     */
    @Select("SELECT * FROM sync_history ORDER BY start_time DESC LIMIT 100")
    List<SyncHistory> selectRecent();
}
