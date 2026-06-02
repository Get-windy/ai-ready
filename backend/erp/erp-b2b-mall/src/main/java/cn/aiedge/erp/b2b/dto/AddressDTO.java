package cn.aiedge.erp.b2b.dto;

import lombok.Data;

@Data
public class AddressDTO {
    private Long id;
    private String consignee;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;
    private String label;
}
