package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 营销自动化规则（会员生命周期触达）。
 *
 * <p>⚠️ 本系统建模：ql361 营销域无对应页（实测仅 17 页）。触发点与动作参照
 * 有赞营销画布 / 微盟营销中心 / SAP Emarsys Win-Back 的通用形态。</p>
 */
@Data
@Accessors(chain = true)
@TableName("mkt_auto_campaign")
public class AutoCampaign {

    // 触发点
    public static final String TRIGGER_NEW_CUSTOMER = "NEW_CUSTOMER";
    public static final String TRIGGER_BIRTHDAY = "BIRTHDAY";
    public static final String TRIGGER_SLEEPING = "SLEEPING";
    public static final String TRIGGER_REPURCHASE = "REPURCHASE";
    public static final String TRIGGER_POINTS_EXPIRING = "POINTS_EXPIRING";
    public static final String TRIGGER_CARD_EXPIRING = "CARD_EXPIRING";

    // 动作
    public static final String ACTION_COUPON = "COUPON";
    public static final String ACTION_SMS = "SMS";
    public static final String ACTION_POINTS = "POINTS";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private String name;
    /** 触发点（见本类常量） */
    private String triggerType;
    /** 触发参数：沉睡天数 / 生日提前天数 / 复购周期天数 / 积分到期前天数 / 卡到期前天数 */
    private Integer triggerDays;

    /** 动作：COUPON / SMS / POINTS */
    private String actionType;
    private Long couponTemplateId;
    private Long smsTemplateId;
    private String smsContent;
    private BigDecimal pointsValue;

    /** 频控：同一会员 N 天内最多触达 M 次（0/NULL = 不限） */
    private Integer freqDays;
    private Integer freqCount;
    /** 同一会员是否只触发一次 */
    private Integer oncePerMember;

    private Integer status;
    private LocalDateTime lastRunTime;
    private Integer lastRunCount;
    private Integer lastRunSuccess;
    private String remark;

    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
