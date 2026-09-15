package cn.aiedge.dms.channel.dto;

import lombok.Data;

/**
 * 渠道配送员统计（Mapper 出参，用于一次查询批量补全运力列，避免 N+1）
 *
 * @author AI-Ready Team
 */
@Data
public class ChannelRiderStatVO {

    private Long channelId;

    /** 配送员总数 */
    private Integer riderTotal;

    /** 在线配送员数（空闲/忙碌） */
    private Integer riderOnline;
}
