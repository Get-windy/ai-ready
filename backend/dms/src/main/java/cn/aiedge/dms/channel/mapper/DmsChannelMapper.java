package cn.aiedge.dms.channel.mapper;

import cn.aiedge.dms.channel.dto.ChannelRiderStatVO;
import cn.aiedge.dms.channel.entity.DmsChannel;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 配送渠道 Mapper
 */
@Mapper
public interface DmsChannelMapper extends BaseMapper<DmsChannel> {

    /**
     * 按渠道统计配送员规模（一次查全量再在内存分组，避免列表页 N+1）。
     * 显式带 deleted / tenant_id：自定义 SQL 不会被 {@code @TableLogic} 与多租户插件自动处理。
     */
    @Select("""
            SELECT channel_id AS channel_id,
                   count(*)::int AS rider_total,
                   count(*) FILTER (WHERE status IN (1, 2))::int AS rider_online
            FROM dms_rider
            WHERE deleted = 0
              AND tenant_id = #{tenantId}
              AND channel_id IS NOT NULL
            GROUP BY channel_id
            """)
    List<ChannelRiderStatVO> selectRiderStat(@Param("tenantId") Long tenantId);

    /** 渠道被配送员引用数（删除前引用保护） */
    @Select("""
            SELECT count(*) FROM dms_rider
            WHERE deleted = 0 AND tenant_id = #{tenantId} AND channel_id = #{channelId}
            """)
    Long countRidersByChannel(@Param("tenantId") Long tenantId, @Param("channelId") Long channelId);

    /**
     * 按编码跨租户查渠道（外部平台回调无登录会话：此时多租户插件不注入条件，
     * 但仍显式忽略租户，避免会话残留导致「回调查不到渠道」）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT * FROM dms_channel
            WHERE deleted = 0 AND channel_code = #{channelCode}
            ORDER BY tenant_id
            """)
    List<DmsChannel> selectByCodeIgnoreTenant(@Param("channelCode") String channelCode);
}
