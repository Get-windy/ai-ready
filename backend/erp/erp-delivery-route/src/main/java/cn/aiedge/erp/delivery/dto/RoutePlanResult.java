package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoutePlanResult {

    private Long routeId;

    private String routeCode;

    private Integer totalPoints;

    private Double totalDistance;

    private Integer totalDuration;

    private List<RoutePointResult> points;

    private String routePolyline;

    private String optimizedOrder;
}