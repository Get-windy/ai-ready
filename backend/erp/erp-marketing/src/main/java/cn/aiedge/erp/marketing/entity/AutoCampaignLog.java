package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 营销自动化执行台账（一规则 × 一会员 = 一行） */
@Data
@Accessors(chain = true)
@TableName("mkt_auto_campaign_log")
public class AutoCampaignLog {

    public static final String SUCCESS = "SUCCESS";
    public static final String SKIPPED = "SKIPPED";
    public static final String FAILED = "FAILED";

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private Long campaignId;
    private String campaignName;
    private String triggerType;
    private String actionType;

    private Long partnerId;
    private String memberName;
    private String mobile;

    /** SUCCESS / SKIPPED / FAILED */
    private String result;
    private String resultMsg;
    private String batchNo;
    /** 触发依据说明（如「沉睡 95 天」「生日还有 3 天」） */
    private String triggerNote;

    private LocalDateTime createTime;
}
