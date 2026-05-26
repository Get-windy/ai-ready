package cn.aiedge.erp.delivery.dto;

import lombok.Data;

@Data
public class NavigationRequest {

    private Long routeId;

    private Integer currentPointOrder;

    private String currentLatitude;

    private String currentLongitude;

    private String targetLatitude;

    private String targetLongitude;
}