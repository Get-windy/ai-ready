package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 「我要推广」各物料 Tab 的分享统计行（按 target_id 聚合） */
@Data
public class ShareSummaryRow {

    private Long targetId;
    /** 最近分享时间 */
    private LocalDateTime lastShareTime;
    /** 分享次数 */
    private Integer shareCount;
    /** 浏览次数 */
    private Integer viewCount;
    /** 浏览人数 */
    private Integer viewerCount;
    /** 分享领取数 */
    private Integer receiveCount;
}
