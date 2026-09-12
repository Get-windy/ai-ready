package cn.aiedge.erp.party.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 互联账号 VO（对标数据列：往来单位 / 互联用户名 / 手机号）
 */
@Getter
@Setter
public class LinkedAccountVO {

    private Long id;

    /** 往来单位ID */
    private Long partyId;

    /** 往来单位编号 */
    private String partyCode;

    /** 往来单位名称（列表「往来单位」列） */
    private String partyName;

    /** 互联平台标识 */
    private String platform;

    /** 互联平台中文（微信 / 支付宝 / 抖音 / 商城 / 其它） */
    private String platformDesc;

    /** 关联类型标识 */
    private String linkType;

    /** 关联类型中文（会员 / 客户 / 其它） */
    private String linkTypeDesc;

    /** 互联用户名（列表「互联用户名」列） */
    private String linkedUserName;

    /** 手机号（列表「手机号」列） */
    private String phone;

    /** 绑定状态：1 已绑定 / 0 已解绑 */
    private Integer status;

    /** 绑定状态中文 */
    private String statusDesc;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
