package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoutePlanRequest {

    private String deliveryPersonId;

    private String deliveryPersonName;

    private String startAddress;

    private String startLatitude;

    private String startLongitude;

    private List<RoutePointRequest> points;

    private String strategy;
}