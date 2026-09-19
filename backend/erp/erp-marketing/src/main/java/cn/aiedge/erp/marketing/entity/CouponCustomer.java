package cn.aiedge.erp.marketing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/** 优惠券指定客户（优惠券模板 × 往来单位，customer_scope = SPECIFIED 时生效） */
@Data
@Accessors(chain = true)
@TableName("mkt_coupon_customer")
public class CouponCustomer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long templateId;
    private Long partnerId;
    private String partnerName;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
}
