package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 短信模板（营销 → 营销活动 → 发短信 →「短信模板管理」Tab） */
@Data
@Accessors(chain = true)
@TableName("mkt_sms_template")
public class SmsTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 模版标题 */
    private String templateTitle;
    /** 模版内容 */
    private String templateContent;
    /** 短信类型：NOTICE 通知短信 / MARKETING 营销短信 */
    private String smsType;
    private Integer status;

    @TableLogic
    private Integer deleted;
    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
