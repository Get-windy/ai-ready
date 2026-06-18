package cn.aiedge.erp.signature.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SignatureRecordDTO {

    private Long id;

    private String signatureCode;

    private String orderNo;

    private Long orderId;

    private String signatureType;

    private String signerName;

    private String signerPhone;

    private String signatureData;

    private String photoPath;

    private String photoUrl;

    private String location;

    private String latitude;

    private String longitude;

    private String deliveryPersonName;

    private LocalDateTime signTime;

    private Boolean verified;

    private Integer status;
}