package cn.aiedge.erp.signature.dto;

import lombok.Data;

@Data
public class PhotoSignatureRequest {

    private String orderNo;

    private Long orderId;

    private String signerName;

    private String signerPhone;

    private String location;

    private String latitude;

    private String longitude;

    private String deviceId;

    private String deliveryPersonId;

    private String deliveryPersonName;

    private String remark;
}