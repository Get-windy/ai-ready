package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 营销短信台账（营销 → 营销活动 → 发短信 →「短信历史」Tab）
 * 投递状态不落本表：由 {@code message_id} join sys_message 读取（单一真源）。
 */
@Data
@Accessors(chain = true)
@TableName("mkt_sms_record")
public class SmsRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    /** 发送批次号 */
    private String batchNo;
    /** 平台消息ID（sys_message.id） */
    private Long messageId;
    private Long receiverId;
    /** 接收人 */
    private String receiverName;
    /** 手机号 */
    private String mobile;
    /** 短信内容（不含签名） */
    private String content;
    /** 短信类型：NOTICE / MARKETING */
    private String smsType;
    /** 公司签名 */
    private String signName;
    private Long handlerId;
    /** 经手人 */
    private String handlerName;
    /** 发送时的合规校验结论（时段/频控/退订名单） */
    private String complianceNote;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
