package cn.aiedge.erp.delivery.dto;

import lombok.Data;

@Data
public class RoutePointResult {

    private Long pointId;

    private Integer pointOrder;

    private String orderId;

    private String orderNo;

    private String customerName;

    private String address;

    private String latitude;

    private String longitude;

    private Double distanceFromPrev;

    private Integer durationFromPrev;

    private String status;
}