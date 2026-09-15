package cn.aiedge.trade.mapper;

import cn.aiedge.trade.entity.ExternalOrderRaw;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 外部订单原始报文 Mapper
 *
 * <p>⚠️ 手写 SQL 必须自带 `deleted = 0`：MyBatis-Plus 的逻辑删除只作用于内置方法，
 * 不会改写注解 SQL（见《MyBatis 两个静默陷阱》）；`external_order_raw.deleted` 由迁移
 * `V11.360.2` 补齐（此前实体有 @TableLogic 而表缺列 → 该表所有查询 500）。</p>
 */
@Mapper
public interface ExternalOrderRawMapper extends BaseMapper<ExternalOrderRaw> {

    @Select("SELECT * FROM external_order_raw WHERE deleted = 0 AND process_status = #{status} "
            + "ORDER BY receive_time ASC LIMIT #{limit}")
    List<ExternalOrderRaw> selectPending(@Param("status") int status, @Param("limit") int limit);

    /** 待处理数（渠道为空 = 全部渠道；此前的 `channel_code = NULL` 恒不匹配，恒返回 0） */
    @Select("<script>SELECT COUNT(*) FROM external_order_raw WHERE deleted = 0 AND process_status = 0"
            + "<if test='channelCode != null and channelCode != \"\"'> AND channel_code = #{channelCode}</if>"
            + "</script>")
    int countPending(@Param("channelCode") String channelCode);

    @Select("SELECT COUNT(*) > 0 FROM external_order_raw WHERE deleted = 0 "
            + "AND channel_code = #{channelCode} AND external_order_id = #{externalOrderId}")
    boolean existsByExternalId(@Param("channelCode") String channelCode,
                              @Param("externalOrderId") String externalOrderId);

    /**
     * 处理状态笔数聚合（真实聚合 SQL）：0待处理 / 1已转换 / 2已入库 / 3失败
     *
     * <p>供 `/api/trade/external-order/stat` 使用；`deleted = 0` 必须手写，
     * tenant_id 条件由租户插件注入。</p>
     */
    @Select("SELECT process_status AS \"processStatus\", COUNT(*)::int AS \"count\" "
            + "FROM external_order_raw WHERE deleted = 0 GROUP BY 1")
    List<Map<String, Object>> countByProcessStatus();
}
