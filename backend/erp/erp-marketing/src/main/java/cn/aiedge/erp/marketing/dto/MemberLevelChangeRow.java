package cn.aiedge.erp.marketing.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 会员等级评估行（评估与执行共用同一口径） */
@Data
public class MemberLevelChangeRow {

    private Long partnerId;
    private String partyCode;
    private String partyName;
    /** 当前等级（biz_party.member_level） */
    private String currentLevel;
    /** 按门槛算出的目标等级 */
    private String targetLevel;
    /** UPGRADE 升级 / DOWNGRADE 降级 / KEEP 不变 / INIT 首次定级 */
    private String action;
    /** 该会员累计消费额 */
    private BigDecimal totalConsume;
    /** 该会员当前积分 */
    private Integer points;
    /** 命中的门槛说明（便于页面解释"为什么升/降到这一级"） */
    private String reason;
}
