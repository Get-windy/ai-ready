package cn.aiedge.erp.party.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 互联账号查询条件（对标：往来单位 + 互联账号两固定项；其余为扩展过滤，不改变对标查询区）
 */
@Getter
@Setter
public class LinkedAccountQuery {

    /** 往来单位（biz_party.id） */
    private Long partyId;

    /** 互联账号：按手机号 / 互联用户名模糊匹配（对标占位「请输入互联手机号码」） */
    private String keyword;

    /** 互联平台标识（全局唯一字典） */
    private String platform;

    /** 关联类型标识（全局唯一字典） */
    private String linkType;

    /** 绑定状态：1 已绑定 / 0 已解绑；null = 全部 */
    private Integer status;
}
