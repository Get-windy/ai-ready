package cn.aiedge.erp.delivery.dto;

import lombok.Data;

@Data
public class AmapRouteStep {

    private Double distance;

    private Integer duration;

    private String polyline;

    private String road;

    private String instruction;
}