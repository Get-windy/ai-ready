package cn.aiedge.erp.marketing.dto;

import lombok.Data;

/** 促销活动「查看客户」行 */
@Data
public class PromoCustomerRow {

    private Long id;
    private String partyCode;
    private String partyName;
    private String memberLevel;
    private String phone;
}
