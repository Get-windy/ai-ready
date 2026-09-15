package cn.aiedge.trade.mapper;

import cn.aiedge.trade.entity.InventorySyncRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface InventorySyncRecordMapper extends BaseMapper<InventorySyncRecord> {

    /**
     * 失败原因分类统计（《API监控开发文档》§3.5.6）
     *
     * <p>⚠️ 手写 SQL 必须自带 `deleted = 0`：MyBatis-Plus 的逻辑删除只作用于内置方法，
     * 不会改写本注解 SQL（见《MyBatis 两个静默陷阱》）。</p>
     */
    @Select("SELECT error_category AS \"category\", COUNT(*)::int AS \"count\" "
            + "FROM inventory_sync_record WHERE deleted = 0 AND error_category IS NOT NULL "
            + "GROUP BY 1 ORDER BY \"count\" DESC, 1")
    List<Map<String, Object>> countByErrorCategory();

    /**
     * 同步状态笔数聚合（真实聚合 SQL）：sync_status 0待同步 / 1成功 / 2失败
     *
     * <p>供 `/api/trade/inventory-sync/stat` 使用（不再依赖管理端当前页计算）；
     * `deleted = 0` 必须手写（逻辑删除不改写注解 SQL），tenant_id 由租户插件注入。</p>
     */
    @Select("SELECT sync_status AS \"syncStatus\", COUNT(*)::int AS \"count\" "
            + "FROM inventory_sync_record WHERE deleted = 0 GROUP BY 1")
    List<Map<String, Object>> countBySyncStatus();
}
