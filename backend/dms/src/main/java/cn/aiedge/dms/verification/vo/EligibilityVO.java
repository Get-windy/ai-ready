package cn.aiedge.dms.verification.vo;

import lombok.Data;

import java.util.List;

/**
 * 骑手接单资质校验结果
 *
 * <p>「无资质不接单」唯一放行口径：认证已通过 + 证照/背书均在有效期内。
 * 供《调度任务》指派/改派、《订单池》抢单联动校验。</p>
 */
@Data
public class EligibilityVO {

    /** 配送员ID */
    private Long riderId;

    /** 是否具备接单资质 */
    private Boolean eligible;

    /** 认证状态：0-待提交 1-待审核 2-已通过 3-已驳回 4-已过期；null=无台账 */
    private Integer verifyStatus;

    /** 认证状态文本 */
    private String verifyStatusText;

    /** 不具备资质的原因列表 */
    private List<String> reasons;
}
