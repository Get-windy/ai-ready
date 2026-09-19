package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 短信发送同意留痕（合规举证：谁 · 何时 · 为哪个客户/号码 · 同意哪一版协议） */
@Data
@Accessors(chain = true)
@TableName("mkt_sms_consent")
public class SmsConsent {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;

    private Long partnerId;
    private String mobile;
    /** 协议版本号（协议文本变更时递增，便于举证"当时同意的是哪一版"） */
    private String agreementVersion;
    private LocalDateTime agreedTime;
    private String agreedBy;
    private String agreedIp;
    private String remark;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
