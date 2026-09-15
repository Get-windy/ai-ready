package cn.aiedge.erp.delivery.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 配送 ETA 通知台账（表 erp_delivery_eta_notify）
 *
 * ⚠️ 能力保留：ETA 计算与「待发送」落库已实现，短信/微信/APP 通道**尚未接入**，
 *    因此记录状态停留在 PENDING；接入通道后由发送任务消费本表并回写 SENT/FAILED。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("erp_delivery_eta_notify")
public class DeliveryEtaNotify {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long routeId;

    private String routeCode;

    private Long pointId;

    private Integer pointSeq;

    private String customerName;

    private String customerPhone;

    private String address;

    /** 预估到达时间 */
    private LocalDateTime etaTime;

    /** SMS / WECHAT / APP / MANUAL */
    private String channel;

    private String content;

    /** PENDING-待发送 SENT-已发送 FAILED-失败 CANCELLED-已取消 */
    private String status;

    /** 投递到消息底座的消息ID（sys_message.id），发送结果由 MessageSendTask 回写 */
    private Long messageId;

    /** 非持久化：消息底座的发送结果（0待发送 1发送中 2成功 3失败），查询时回填 */
    @TableField(exist = false)
    private Integer channelStatus;

    private Integer retryCount;

    private String errorMsg;

    private LocalDateTime sentTime;

    private String createByName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
