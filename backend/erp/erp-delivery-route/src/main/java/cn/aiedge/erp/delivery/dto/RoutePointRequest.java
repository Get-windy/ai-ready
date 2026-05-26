package cn.aiedge.erp.delivery.dto;

import lombok.Data;

@Data
public class RoutePointRequest {

    private String orderId;

    private String orderNo;

    private String customerName;

    private String customerPhone;

    private String address;

    private String latitude;

    private String longitude;

    private Integer priority;
}