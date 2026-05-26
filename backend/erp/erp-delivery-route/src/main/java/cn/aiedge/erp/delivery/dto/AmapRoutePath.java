package cn.aiedge.erp.delivery.dto;

import lombok.Data;

import java.util.List;

@Data
public class AmapRoutePath {

    private Double distance;

    private Integer duration;

    private List<AmapRouteStep> steps;
}