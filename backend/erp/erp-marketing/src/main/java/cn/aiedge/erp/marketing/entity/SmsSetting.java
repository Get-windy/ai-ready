package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 短信设置（单行：公司签名 + 短信配额）——营销 → 营销活动 → 发短信 */
@Data
@Accessors(chain = true)
@TableName("mkt_sms_setting")
public class SmsSetting {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 公司签名（发送时以【签名】前缀拼入） */
    private String signName;
    /** 短信总配额（条） */
    private Integer quotaTotal;
    /** 已使用（条） */
    private Integer quotaUsed;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 剩余短信条数（派生列，不落库） */
    @TableField(exist = false)
    private Integer quotaRemain;

    // ── 合规设置（V11.379.0）──
    /** 允许发送起始小时（默认 8 = 8:00） */
    private Integer sendStartHour;
    /** 允许发送截止小时（默认 21 = 21:00） */
    private Integer sendEndHour;
    /** 频控窗口天数（默认 7） */
    private Integer freqLimitDays;
    /** 窗口内同一手机号最多接收条数（默认 3） */
    private Integer freqLimitCount;
}
