package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商城拼团 →「拼团活动」Tab 行（对标 9 列）：
 * 活动ID / 活动名称 / 起始时间 / 结束时间 / 成团类型 / 开团个数 / 成功团个数 / 活动状态 / 创建时间
 */
@Data
public class GroupBuyActivityRowVO {

    private Long id;
    /** 活动ID（展示用活动编码，无编码时回退主键） */
    private String activityCode;
    private Long rawId;
    private String activityName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer minGroupSize;
    private Integer maxGroupSize;
    /** 成团类型（由 min/max 成团人数派生） */
    private String groupType;
    /** 开团个数（distinct group_id） */
    private Integer groupCount;
    /** 成功团个数 */
    private Integer successGroupCount;
    private String status;
    private LocalDateTime createTime;

    private Long productId;
    private BigDecimal groupPrice;
}
