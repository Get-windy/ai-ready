package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 短信退订名单。
 *
 * <p>合规要点：客户回复「R」退订或明确拒绝后**必须立即停止发送，且不得换名义再发**
 * （《网络交易监督管理办法》第 16 条）。</p>
 */
@Data
@Accessors(chain = true)
@TableName("mkt_sms_opt_out")
public class SmsOptOut {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private String mobile;
    private String receiverName;
    /** 退订来源：REPLY_R 回复R / MANUAL 人工登记 / CUSTOMER 客户主动要求 */
    private String source;
    private LocalDateTime optOutTime;
    private String remark;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
