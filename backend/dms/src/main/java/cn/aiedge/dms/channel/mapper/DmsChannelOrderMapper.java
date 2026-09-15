package cn.aiedge.dms.channel.mapper;

import cn.aiedge.dms.channel.entity.DmsChannelOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 渠道外部单台账 Mapper
 */
@Mapper
public interface DmsChannelOrderMapper extends BaseMapper<DmsChannelOrder> {

    /**
     * 按幂等键取台账（显式带 deleted：自定义 SQL 不会被 {@code @TableLogic} 自动处理；
     * tenant_id 由多租户插件注入，回调场景需先设置临时租户上下文）。
     */
    @Select("""
            SELECT * FROM dms_channel_order
            WHERE deleted = 0 AND idem_key = #{idemKey}
            ORDER BY id DESC
            LIMIT 1
            """)
    DmsChannelOrder selectByIdemKey(@Param("idemKey") String idemKey);

    /** 按外部平台单号取台账（回调关联） */
    @Select("""
            SELECT * FROM dms_channel_order
            WHERE deleted = 0 AND channel_id = #{channelId} AND channel_order_no = #{channelOrderNo}
            ORDER BY id DESC
            LIMIT 1
            """)
    DmsChannelOrder selectByChannelOrderNo(@Param("channelId") Long channelId,
                                           @Param("channelOrderNo") String channelOrderNo);
}
